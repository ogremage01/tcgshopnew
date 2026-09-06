package com.shop.config.mainPageContent.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.shop.config.mainPageContent.dto.ChangeMainPageContentOrderDto;
import com.shop.config.mainPageContent.dto.MainPageContentDto;
import com.shop.config.mainPageContent.dto.SaveMainPageContentDto;
import com.shop.config.mainPageContent.entity.MainPageContent;
import com.shop.config.mainPageContent.repository.MainPageContentRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class MainPageContentServiceImpl implements MainPageContentService {

    private static final Pattern IMG_SRC_PATTERN = Pattern.compile("<img[^>]+src=\"([^\"]+)\"", Pattern.CASE_INSENSITIVE);

    private final MainPageContentRepository mainPageContentRepository;

    @Override
    public List<MainPageContentDto> findAll() {
        return mainPageContentRepository.findAllByActiveTrueOrderByDisplayOrderAsc().stream()
                .map(MainPageContent::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void add(SaveMainPageContentDto dto) {
        mainPageContentRepository.incrementAllDisplayOrders();

        MainPageContent content = MainPageContent.builder()
                .name(dto.getName())
                .content(dto.getContent())
                .imageUrl(extractFirstImageUrl(dto.getContent()))
                .link(dto.getLink())
                .displayOrder(1)
                .active(true)
                .build();

        mainPageContentRepository.save(content);
    }

    @Override
    @Transactional
    public void update(Long id, SaveMainPageContentDto dto) {
        MainPageContent content = mainPageContentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("콘텐츠를 찾을 수 없습니다. id=" + id));

        content.setName(dto.getName());
        content.setContent(dto.getContent());
        content.setImageUrl(extractFirstImageUrl(dto.getContent()));
        content.setLink(dto.getLink());

        mainPageContentRepository.save(content);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        mainPageContentRepository.softDeleteById(id);
    }

    @Override
    @Transactional
    public void changeOrder(ChangeMainPageContentOrderDto dto) {
        if (dto == null || dto.getOrderedIds() == null || dto.getOrderedIds().isEmpty()) {
            throw new RuntimeException("orderedIds is empty");
        }

        Map<Long, Integer> displayOrderById = new HashMap<>();
        for (int i = 0; i < dto.getOrderedIds().size(); i++) {
            displayOrderById.put(dto.getOrderedIds().get(i), i + 1);
        }

        List<MainPageContent> contents = mainPageContentRepository.findAllByActiveTrueOrderByDisplayOrderAsc();

        for (MainPageContent content : contents) {
            Integer displayOrder = displayOrderById.get(content.getId());
            if (displayOrder != null) {
                content.setDisplayOrder(displayOrder);
            }
        }

        mainPageContentRepository.saveAll(contents);
    }

    private String extractFirstImageUrl(String html) {
        if (html == null || html.isBlank()) {
            return null;
        }
        Matcher matcher = IMG_SRC_PATTERN.matcher(html);
        if (matcher.find()) {
            String src = matcher.group(1);
            return src.isBlank() ? null : src;
        }
        return null;
    }
}
