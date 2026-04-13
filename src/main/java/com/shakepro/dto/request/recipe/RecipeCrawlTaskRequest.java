package com.shakepro.dto.request.recipe;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RecipeCrawlTaskRequest {

    @NotBlank(message = "来源站点不能为空")
    @Size(max = 50, message = "来源站点最长50位")
    private String sourceSite;

    @NotBlank(message = "入口链接不能为空")
    @Size(max = 255, message = "入口链接最长255位")
    private String entryUrl;

    @Size(max = 20, message = "抓取模式最长20位")
    private String crawlMode;

    @Min(value = 1, message = "抓取页数不能小于1")
    @Max(value = 100, message = "抓取页数不能大于100")
    private Integer maxPages;

    @Min(value = 1, message = "抓取数量不能小于1")
    @Max(value = 500, message = "抓取数量不能大于500")
    private Integer maxItems;

    private Boolean fetchDetailPages;
}
