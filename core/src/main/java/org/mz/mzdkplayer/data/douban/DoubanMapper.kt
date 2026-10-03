package org.mz.mzdkplayer.data.douban

import org.mz.mzdkplayer.data.local.MediaCacheEntity
import org.mz.mzdkplayer.data.model.MediaItem
import org.mz.mzdkplayer.tool.logic.DoubanLogic

/**
 * 豆瓣原始 DTO → 归一化模型。纯函数，被 `DoubanScraper` 调用，也被单测直接覆盖。
 */

/** 演员最多保留 15 位、导演保留 3 位，和 TMDB 详情页展示量级保持一致 */
private const val MAX_ACTORS = 15
private const val MAX_DIRECTORS = 3

internal fun DoubanSuggestDto.toCandidate(): DoubanLogic.Candidate? {
    val subjectId = id?.trim().orEmpty()
    if (subjectId.isEmpty()) return null
    return DoubanLogic.Candidate(
        id = subjectId,
        title = DoubanLogic.cleanTitle(title),
        year = DoubanLogic.toYear(year),
        // `episode` 非空 = 有集数，是搜索结果里唯一能暗示“剧集”的信号（`type` 字段不可信）
        hasEpisode = !episode.isNullOrBlank(),
        posterUrl = DoubanLogic.largePosterUrl(img),
    )
}

/**
 * 手动匹配页（`EditTMDBInfoScreen` / `PhoneMatchScreen`）里的一行搜索结果。
 *
 * 两个约定与入库时保持同一口径：
 * 1. 豆瓣条目 id 借存在 [MediaItem.id] 里，靠 [MediaItem.source] 与 TMDB 的 id 区分（两边的
 *    数值区间是重叠的）；id 不是数字串说明接口变了，这一条直接丢掉。
 * 2. 类型只能先按 `episode` 猜（`type` 字段不可信，见 [DoubanLogic] 的说明）：有集数当剧集，
 *    没有当电影。点选之后还会用详情接口的 subtype 再核对一次，猜错不会落库落错库。
 */
internal fun DoubanLogic.Candidate.toMediaItem(): MediaItem? {
    val subjectId = id.toIntOrNull() ?: return null
    return MediaItem(
        id = subjectId,
        title = title,
        overview = "",
        posterPath = posterUrl,
        releaseDate = year?.toString(),
        isMovie = !hasEpisode,
        source = MediaCacheEntity.SOURCE_DOUBAN,
    )
}

internal fun DoubanSubjectDto.toSubject(): DoubanSubject? {
    val subjectId = id?.trim().orEmpty()
    if (subjectId.isEmpty()) return null

    val displayTitle = DoubanLogic.cleanTitle(title).ifEmpty { DoubanLogic.cleanTitle(originalTitle) }
    if (displayTitle.isEmpty()) return null

    val displayYear = DoubanLogic.toYear(year) ?: DoubanLogic.toYear(releaseDate)
    val isSeries = isTv == true || subtype.equals("tv", ignoreCase = true)

    return DoubanSubject(
        id = subjectId,
        title = displayTitle,
        originalTitle = DoubanLogic.cleanTitle(originalTitle).takeIf { it.isNotEmpty() && it != displayTitle },
        year = displayYear,
        isTv = isSeries,
        overview = intro?.trim().orEmpty(),
        // 优先 pic.large（m 档，升到 l 档更清晰），没有再用 cover_url
        posterUrl = DoubanLogic.largePosterUrl(pic?.large) ?: DoubanLogic.largePosterUrl(coverUrl),
        backdropUrl = null,
        // 豆瓣评分满分为 10，和 TMDB 一致，不做换算
        rating = rating?.value?.takeIf { it > 0.0 } ?: 0.0,
        genres = genres.orEmpty().map { it.trim() }.filter { it.isNotEmpty() },
        countries = countries.orEmpty().map { it.trim() }.filter { it.isNotEmpty() },
        languages = languages.orEmpty().map { it.trim() }.filter { it.isNotEmpty() },
        releaseDate = DoubanLogic.releaseDate(pubdate, displayYear) ?: releaseDate?.trim()?.takeIf { it.isNotEmpty() },
        duration = DoubanLogic.firstDuration(durations),
        episodesCount = episodesCount?.takeIf { it > 0 } ?: lastEpisodeNumber?.takeIf { it > 0 },
        directors = DoubanLogic.creditNames(directors?.map { it.name }, MAX_DIRECTORS),
        actors = DoubanLogic.creditNames(actors?.map { it.name }, MAX_ACTORS),
    )
}

/** 取第一张宽幅剧照的 `image.large.url` 作为背景图 */
internal fun DoubanPhotosDto.firstWideImageUrl(): String? =
    photos.orEmpty()
        .asSequence()
        .mapNotNull { it.image?.large?.url }
        .mapNotNull { DoubanLogic.backdropUrl(it) }
        .firstOrNull()
