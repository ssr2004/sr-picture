package com.tuque.srpicturebackend.service.impl;

import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingStore;
import com.tuque.srpicturebackend.service.PictureSearchService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 图片语义搜索服务实现
 */
@Service
@Slf4j
@ConditionalOnProperty(prefix = "picture.search", name = "vector-enabled", havingValue = "true")
public class PictureSearchServiceImpl implements PictureSearchService {

    @Resource
    private EmbeddingModel embeddingModel;

    @Resource
    private EmbeddingStore<TextSegment> embeddingStore;

    /**
     * 索引图片
     * @param pictureId 图片 ID
     * @param description 图片描述
     */
    private static final String ID_PREFIX = "pic_id:";

    @Override
    public void indexPicture(Long pictureId, String description) {
        try {
            // 将 pictureId 编码到文本中，避免 metadata 读取 bug
            String text = ID_PREFIX + pictureId + " " + description;
            TextSegment segment = TextSegment.from(text);
            Embedding embedding = embeddingModel.embed(segment).content();
            embeddingStore.add(embedding, segment);
            log.info("图片向量化成功，pictureId={}, description={}", pictureId, description);
        } catch (Exception e) {
            log.error("图片向量化失败，pictureId={}", pictureId, e);
        }
    }

    private static final Pattern ID_PATTERN = Pattern.compile("pic_id:(\\d+)");

    @Override
    public List<Long> searchPictureIds(String query, Long spaceId) {
        try {
            Embedding queryEmbedding = embeddingModel.embed(query).content();
            EmbeddingSearchRequest searchRequest = EmbeddingSearchRequest.builder()
                    .queryEmbedding(queryEmbedding)
                    .maxResults(20)
                    .minScore(0.80)
                    .build();
            List<EmbeddingMatch<TextSegment>> matches = embeddingStore.search(searchRequest).matches();
            log.info("语义搜索命中 {} 条结果，query={}", matches.size(), query);
            // 从文本中提取 pictureId
            List<Long> result = new ArrayList<>();
            for (EmbeddingMatch<TextSegment> match : matches) {
                if (match.embedded() == null) continue;
                String text = match.embedded().text();
                log.info("匹配结果：score={}, text={}", match.score(), text);
                Matcher matcher = ID_PATTERN.matcher(text);
                if (matcher.find()) {
                    result.add(Long.parseLong(matcher.group(1)));
                }
            }
            return result.stream().distinct().collect(Collectors.toList());
        } catch (Exception e) {
            log.error("语义搜索失败，query={}", query, e);
            return List.of();
        }
    }
}
