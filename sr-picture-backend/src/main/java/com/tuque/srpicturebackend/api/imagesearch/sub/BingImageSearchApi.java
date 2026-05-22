package com.tuque.srpicturebackend.api.imagesearch.sub;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.tuque.srpicturebackend.api.imagesearch.model.ImageSearchResult;
import com.tuque.srpicturebackend.exception.BusinessException;
import com.tuque.srpicturebackend.exception.ErrorCode;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
@Deprecated
public class BingImageSearchApi {

    private static final Logger log = LoggerFactory.getLogger(BingImageSearchApi.class);

    private static final String BING_SEARCH_URL = "https://cn.bing.com/images/search";

    /**
     * 使用 Bing 以图搜图
     * @param imageUrl 图片地址
     * @return 搜索结果列表
     */
    public static List<ImageSearchResult> search(String imageUrl) {
        try {
            // 构建搜索 URL - 使用 Bing 视觉搜索格式
            String encodedUrl = URLEncoder.encode(imageUrl, StandardCharsets.UTF_8);
            String searchUrl = BING_SEARCH_URL + "?view=detailv2&iss=sbiupload&FORM=SBIIRP&q=imgurl:" + encodedUrl;

            log.info("Bing 以图搜图请求 URL: {}", searchUrl);

            // 使用 Jsoup 发送请求（自动处理重定向）
            Document doc = Jsoup.connect(searchUrl)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
                    .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
                    .timeout(15000)
                    .followRedirects(true)
                    .get();

            // 打印页面标题用于调试
            log.info("Bing 搜索页面标题: {}", doc.title());
            log.info("Bing 搜索页面 URL: {}", doc.location());

            return parseHtml(doc);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Bing 以图搜图失败", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "搜索失败");
        }
    }

    /**
     * 解析 Bing 搜索结果
     */
    private static List<ImageSearchResult> parseHtml(Document doc) {
        List<ImageSearchResult> results = new ArrayList<>();

        // Bing 图片搜索结果通常在 .iusc 元素中，包含 m 属性（JSON 格式）
        Elements iuscElements = doc.select(".iusc");
        for (Element element : iuscElements) {
            try {
                String mAttr = element.attr("m");
                if (mAttr != null && !mAttr.isEmpty()) {
                    JSONObject mJson = JSONUtil.parseObj(mAttr);
                    ImageSearchResult result = new ImageSearchResult();

                    // 提取缩略图 URL
                    String thumbUrl = mJson.getStr("turl");
                    if (thumbUrl != null && !thumbUrl.isEmpty()) {
                        result.setThumbUrl(thumbUrl);
                    }

                    // 提取来源页面 URL
                    String fromUrl = mJson.getStr("purl");
                    if (fromUrl != null && !fromUrl.isEmpty()) {
                        result.setFromUrl(fromUrl);
                    }

                    // 只有当至少有一个 URL 时才添加结果
                    if (result.getThumbUrl() != null || result.getFromUrl() != null) {
                        results.add(result);
                    }
                }
            } catch (Exception e) {
                // 忽略单个结果的解析错误
                log.debug("解析单个结果失败: {}", e.getMessage());
            }
        }

        // 如果 .iusc 没有结果，尝试其他选择器
        if (results.isEmpty()) {
            // 尝试从 img 标签中提取
            Elements imgElements = doc.select("img.mimg, img[data-src]");
            for (Element img : imgElements) {
                try {
                    String src = img.attr("data-src");
                    if (src == null || src.isEmpty()) {
                        src = img.attr("src");
                    }
                    if (src != null && !src.isEmpty() && !src.startsWith("data:")) {
                        ImageSearchResult result = new ImageSearchResult();
                        result.setThumbUrl(src);

                        // 尝试获取父元素的链接
                        Element parentLink = img.closest("a[href]");
                        if (parentLink != null) {
                            result.setFromUrl(parentLink.attr("href"));
                        }

                        results.add(result);
                    }
                } catch (Exception e) {
                    log.debug("解析图片元素失败: {}", e.getMessage());
                }
            }
        }

        return results;
    }

    public static void main(String[] args) {
        // 测试用的图片 URL
        String imageUrl = "https://www.codefather.cn/logo.png";
        System.out.println("开始搜索图片: " + imageUrl);
        List<ImageSearchResult> results = search(imageUrl);
        System.out.println("搜索结果数量：" + results.size());
        for (int i = 0; i < results.size(); i++) {
            ImageSearchResult result = results.get(i);
            System.out.println("结果 " + (i + 1) + ":");
            System.out.println("  缩略图：" + result.getThumbUrl());
            System.out.println("  来源：" + result.getFromUrl());
        }
    }
}
