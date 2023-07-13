package com.pyonsnalcolor.batch.service.cu;

import com.pyonsnalcolor.exception.PyonsnalcolorBatchException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.SocketTimeoutException;

import static com.pyonsnalcolor.exception.model.BatchErrorCode.*;
import static com.pyonsnalcolor.exception.model.BatchErrorCode.BATCH_UNAVAILABLE;

public interface CuDescriptionBatch {

    String CU_DESCRIPTION_PAGE_URL = "https://cu.bgfretail.com/product/view.do";
    int TIMEOUT = 15000;

    default String getDescription(Element element, String productType) throws Exception {
        String productCode = getProductCode(element);

        try {
            String detailPage = UriComponentsBuilder
                    .fromUriString(CU_DESCRIPTION_PAGE_URL)
                    .queryParam("category", productType)
                    .queryParam("gdIdx", productCode)
                    .build()
                    .toString();
            Document doc = Jsoup.connect(detailPage).timeout(TIMEOUT).get();
            Elements elements = doc.select("ul.prodExplain li");
            return elements.text();
        } catch (IllegalArgumentException e) {
            throw new PyonsnalcolorBatchException(INVALID_ACCESS, e);
        } catch (SocketTimeoutException e) {
            throw new PyonsnalcolorBatchException(TIME_OUT, e);
        } catch (IOException e) {
            throw new PyonsnalcolorBatchException(IO_EXCEPTION, e);
        } catch (Exception e) {
            throw new PyonsnalcolorBatchException(BATCH_UNAVAILABLE, e);
        }
    }

    default String getProductCode(Element element) {
        String originProductElement = element.select("a").first().attr("href");
        return originProductElement.split("\\(")[1].split("\\)")[0];
    }
}