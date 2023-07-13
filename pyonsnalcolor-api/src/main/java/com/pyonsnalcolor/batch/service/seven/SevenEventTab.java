package com.pyonsnalcolor.batch.service.seven;

import com.pyonsnalcolor.exception.PyonsnalcolorBatchException;
import com.pyonsnalcolor.exception.model.BatchErrorCode;
import com.pyonsnalcolor.product.entity.BaseEventProduct;
import com.pyonsnalcolor.product.enumtype.Category;
import com.pyonsnalcolor.product.enumtype.EventType;
import com.pyonsnalcolor.product.enumtype.StoreType;
import com.pyonsnalcolor.product.enumtype.Tag;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static com.pyonsnalcolor.exception.model.BatchErrorCode.*;
import static com.pyonsnalcolor.product.entity.UUIDGenerator.generateId;

@Slf4j
public enum SevenEventTab {

    ONE_TO_ONE(1, 0),
    TWO_TO_ONE(2, 0),
    GIFT(3, 1),
    DISCOUNT(4, 0);

    @Getter
    private int tab;
    @Getter
    private int startPageIndex;

    private static final String SEVEN_DISCOUNT_URL = "https://www.7-eleven.co.kr/product/presentView.asp";
    private static final String IMG_PREFIX = "https://www.7-eleven.co.kr";
    private static final int TIMEOUT = 10000;

    SevenEventTab(int tab, int startPageIndex) {
        this.tab = tab;
        this.startPageIndex = startPageIndex;
    }

    public String getDocumentTag() {
        if (this == GIFT) {
            return "div.pic_product";
        }
        return "div.pic_product div.pic_product";
    }

    public List<BaseEventProduct> getPagedProducts(Elements elements, Elements detailPageElements) {
        if (this == GIFT) {
            return getPagedProductsByGift(elements);
        }
        if (this == DISCOUNT) {
            return getPagedProductsByDiscount(elements, detailPageElements);
        }
        return elements.stream()
                .map (p -> convertToBaseEventProductWithSubProduct(p, null))
                .collect(Collectors.toList());
    }

    private BaseEventProduct convertToBaseEventProductWithSubProduct(Element originElement, Element subElement) {
        String name = originElement.select("div.name").first().text();
        String image = IMG_PREFIX + originElement.select("img").first().attr("src");
        String price = originElement.select("div.price").text();
        EventType eventType = EventType.getEventTypeWithValue(this.name());

        String originPrice = null;
        if (this == DISCOUNT) {
            String originProductCode = getProductCode(subElement);
            originPrice = getOriginPrice(originProductCode);
        }
        String giftImage = null;
        if (this == GIFT) {
            giftImage = IMG_PREFIX + subElement.select("img").first().attr("src");
        }
        Category category = Category.matchCategoryByProductName(name);
        Tag tag = Tag.findTag(name);

        return BaseEventProduct.builder()
                .name(name)
                .id(generateId())
                .image(image)
                .price(price)
                .giftImage(giftImage)
                .originPrice(originPrice)
                .updatedTime(LocalDateTime.now())
                .eventType(eventType)
                .storeType(StoreType.SEVEN_ELEVEN)
                .category(category)
                .tag(tag)
                .build();
    }

    private List<BaseEventProduct> getPagedProductsByGift(Elements elements) {
        List<BaseEventProduct> pagedProducts = new ArrayList<>();
        AtomicInteger counter = new AtomicInteger();

        elements.stream()
                .collect(Collectors.groupingBy(it -> counter.getAndIncrement() / 2))
                .forEach((classifier, products) -> {
                    Element originElement = products.get(0);
                    Element giftElement = products.get(1);
                    BaseEventProduct baseEventProduct = convertToBaseEventProductWithSubProduct(originElement, giftElement);
                    pagedProducts.add(baseEventProduct);
                });
        return pagedProducts;
    }

    private List<BaseEventProduct> getPagedProductsByDiscount(Elements elements, Elements detailPageElements) {
        List<BaseEventProduct> products = new ArrayList<>();
        int elementSize = 0;
        while (elements.size() != elementSize) {
            Element element = elements.get(elementSize);
            Element discountElement = detailPageElements.get(elementSize);
            products.add(convertToBaseEventProductWithSubProduct(element, discountElement));
            elementSize++;
        }
        return products;
    }

    private String getProductCode(Element element) {
        String originProductElement = element.select("a").first().attr("href");
        return originProductElement.split("\\'")[1];
    }

    private String getOriginPrice(String originProductCode) {
        try {
            String originPriceUrl = UriComponentsBuilder
                    .fromUriString(SEVEN_DISCOUNT_URL)
                    .queryParam("pCd", originProductCode)
                    .build()
                    .toString();
            Document doc = Jsoup.connect(originPriceUrl).timeout(TIMEOUT).get();
            Elements elements = doc.select("div.cont_top span.product_price del");
            return elements.text().replace(" >", "");
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
}