package org.mz.mzdkplayer.data.douban

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
