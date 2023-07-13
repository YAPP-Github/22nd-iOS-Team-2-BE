package com.pyonsnalcolor.batch.service.cu;

import com.pyonsnalcolor.batch.service.EventBatchService;
import com.pyonsnalcolor.exception.PyonsnalcolorBatchException;
import com.pyonsnalcolor.product.entity.BaseEventProduct;
import com.pyonsnalcolor.product.enumtype.Category;
import com.pyonsnalcolor.product.enumtype.EventType;
import com.pyonsnalcolor.product.enumtype.StoreType;
import com.pyonsnalcolor.product.enumtype.Tag;
import com.pyonsnalcolor.product.repository.EventProductRepository;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.pyonsnalcolor.exception.model.BatchErrorCode.*;
import static com.pyonsnalcolor.product.entity.UUIDGenerator.generateId;

@Service("CuEvent")
@Slf4j
public class CuEventBatchService extends EventBatchService implements CuDescriptionBatch {

    private static final String CU_EVENT_URL = "https://cu.bgfretail.com/event/plusAjax.do?";
    private static final String DOC_SELECT_TAG = "a.prod_item";
    private static final int TIMEOUT = 15000;
    private static final String SCHEMA = "https:";

    public CuEventBatchService(EventProductRepository eventProductRepository) {
        super(eventProductRepository);
    }

    @Override
    protected List<BaseEventProduct> getAllProducts() {

        try {
            return getProducts();
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

    private List<BaseEventProduct> getProducts() throws Exception {
        List<BaseEventProduct> products = new ArrayList<>();

        int pageIndex = 1;
        while (true) {
            String pagedCuEventUrl = getCuEventUrlByPageIndex(pageIndex);
            Document doc = Jsoup.connect(pagedCuEventUrl).timeout(TIMEOUT).get();
            Elements elements = doc.select(DOC_SELECT_TAG);

            if (elements.isEmpty()) {
                break;
            }

            List<BaseEventProduct> pagedProducts = elements.stream()
                    .map(this::convertToBaseEventProduct)
                    .collect(Collectors.toList());
            products.addAll(pagedProducts);
            pageIndex += 1;
        }
        return products;
    }

    private BaseEventProduct convertToBaseEventProduct(Element element) {
        String name = element.select("div.name").first().text();
        String image =element.select("img.prod_img").first().attr("src");
        if (!image.contains("http")) {
            image = SCHEMA + image;
        }
        String price = element.select("div.price > strong").first().text();
        String eventTypeTag = element.select("div.badge").first().text();
        EventType eventType = getCuEventType(eventTypeTag);

        String description = null;
        try {
            description = getDescription(element, "event");
        } catch (IllegalArgumentException e) {
            throw new PyonsnalcolorBatchException(INVALID_ACCESS, e);
        } catch (SocketTimeoutException e) {
            throw new PyonsnalcolorBatchException(TIME_OUT, e);
        } catch (IOException e) {
            throw new PyonsnalcolorBatchException(IO_EXCEPTION, e);
        } catch (Exception e) {
            throw new PyonsnalcolorBatchException(BATCH_UNAVAILABLE, e);
        }
        Category category = Category.matchCategoryByProductName(name);
        Tag tag = Tag.findTag(name);

        return BaseEventProduct.builder()
                .id((generateId()))
                .name(name)
                .image(image)
                .price(price)
                .description(description)
                .eventType(eventType)
                .storeType(StoreType.CU)
                .updatedTime(LocalDateTime.now())
                .category(category)
                .tag(tag)
                .build();
    }

    private String getCuEventUrlByPageIndex(int pageIndex) {
        return UriComponentsBuilder
                .fromUriString(CU_EVENT_URL)
                .queryParam("pageIndex", pageIndex)
                .build()
                .toString();
    }

    private EventType getCuEventType(String eventTypeTag) {
        if (eventTypeTag.equals("1+1")) {
            return EventType.ONE_TO_ONE;
        }
        if (eventTypeTag.equals("2+1")) {
            return EventType.TWO_TO_ONE;
        }
        throw new PyonsnalcolorBatchException(INVALID_PRODUCT_TYPE,
                new IllegalArgumentException("CU 이벤트 타입이 기존 엔티티와 다릅니다."));
    }
}