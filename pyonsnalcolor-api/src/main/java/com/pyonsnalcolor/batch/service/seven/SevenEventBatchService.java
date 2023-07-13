package com.pyonsnalcolor.batch.service.seven;

import com.pyonsnalcolor.batch.service.EventBatchService;
import com.pyonsnalcolor.exception.PyonsnalcolorBatchException;
import com.pyonsnalcolor.product.entity.BaseEventProduct;
import com.pyonsnalcolor.product.repository.EventProductRepository;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static com.pyonsnalcolor.exception.model.BatchErrorCode.*;
import static com.pyonsnalcolor.exception.model.BatchErrorCode.BATCH_UNAVAILABLE;

@Service("SevenEvent")
@Slf4j
public class SevenEventBatchService extends EventBatchService {

    private static final String SEVEN_URL = "https://www.7-eleven.co.kr/product/listMoreAjax.asp?intPageSize=10";
    private static final int TIMEOUT = 5000;

    public SevenEventBatchService(EventProductRepository eventProductRepository) {
        super(eventProductRepository);
    }

    @Override
    protected List<BaseEventProduct> getAllProducts() {
        List<BaseEventProduct> products = new ArrayList<>();

        Arrays.stream(SevenEventTab.values())
                .sequential()
                .map(this::getProductsAllTab)
                .filter(Objects::nonNull)
                .forEach(products::addAll);
        return products;
    }

    private List<BaseEventProduct> getProductsAllTab(SevenEventTab sevenEventTab) {
        try {
            return getProductsByTab(sevenEventTab);
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

    public List<BaseEventProduct> getProductsByTab(SevenEventTab sevenEventTab) throws IOException {
        List<BaseEventProduct> products = new ArrayList<>();

        int pageIndex = sevenEventTab.getStartPageIndex();
        int tab = sevenEventTab.getTab();
        while (true) {
            String sevenEventUrl = getSevenEventUrlByPageIndexAndTab(pageIndex, tab);
            Document document = Jsoup.connect(sevenEventUrl).timeout(TIMEOUT).get();
            Elements elements = document.select(sevenEventTab.getDocumentTag());

            if (elements.isEmpty()) {
                break;
            }
            Document detailPageDocument = Jsoup.connect(sevenEventUrl).timeout(TIMEOUT).get();
            Elements detailPageElements = detailPageDocument.select("a.btn_product_01");

            List<BaseEventProduct> pagedProducts = sevenEventTab.getPagedProducts(elements, detailPageElements);
            products.addAll(pagedProducts);
            pageIndex += 1;
        }
        return products;
    }

    private String getSevenEventUrlByPageIndexAndTab(int pageIndex, int tab) {
        return UriComponentsBuilder
                .fromUriString(SEVEN_URL)
                .queryParam("intCurrPage", pageIndex)
                .queryParam("pTab", tab)
                .build()
                .toString();
    }
}