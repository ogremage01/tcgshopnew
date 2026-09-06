package com.shop.config.mainHeader.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shop.config.mainHeader.dto.ChangeMainHeaderOrderDto;
import com.shop.config.mainHeader.dto.MainHeaderDto;
import com.shop.config.mainHeader.dto.SaveMainHeaderDto;
import com.shop.config.mainHeader.entity.MainHeader;
import com.shop.config.mainHeader.repository.MainHeaderRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MainHeaderServiceImpl implements MainHeaderService {

    private final MainHeaderRepository mainHeaderRepository;

    @Override
    public List<MainHeaderDto> findAll() {
        return mainHeaderRepository.findAllByOrderByDisplayOrderAsc().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<MainHeaderDto> findActive() {
        return mainHeaderRepository.findAllByIsActiveTrueOrderByDisplayOrderAsc().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void add(SaveMainHeaderDto dto) {
        validate(dto);

        mainHeaderRepository.incrementAllDisplayOrders();

        MainHeader header = MainHeader.builder()
                .title(dto.getTitle().trim())
                .urlString(dto.getUrlString().trim())
                .isActive(dto.getIsActive() == null || Boolean.TRUE.equals(dto.getIsActive()))
                .displayOrder(1)
                .build();

        mainHeaderRepository.save(header);
    }

    @Override
    @Transactional
    public void update(Long id, SaveMainHeaderDto dto) {
        validate(dto);

        MainHeader header = mainHeaderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("헤더 메뉴를 찾을 수 없습니다. id=" + id));

        header.setTitle(dto.getTitle().trim());
        header.setUrlString(dto.getUrlString().trim());
        if (dto.getIsActive() != null) {
            header.setIsActive(dto.getIsActive());
        }

        mainHeaderRepository.save(header);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (!mainHeaderRepository.existsById(id)) {
            throw new RuntimeException("헤더 메뉴를 찾을 수 없습니다. id=" + id);
        }
        mainHeaderRepository.deleteById(id);
    }

    @Override
    @Transactional
    public void changeOrder(ChangeMainHeaderOrderDto dto) {
        if (dto == null || dto.getOrderedIds() == null || dto.getOrderedIds().isEmpty()) {
            throw new RuntimeException("orderedIds is empty");
        }

        Map<Long, Integer> displayOrderById = new HashMap<>();
        for (int i = 0; i < dto.getOrderedIds().size(); i++) {
            displayOrderById.put(dto.getOrderedIds().get(i), i + 1);
        }

        List<MainHeader> headers = mainHeaderRepository.findAllByOrderByDisplayOrderAsc();
        for (MainHeader header : headers) {
            Integer displayOrder = displayOrderById.get(header.getId());
            if (displayOrder != null) {
                header.setDisplayOrder(displayOrder);
            }
        }

        mainHeaderRepository.saveAll(headers);
    }

    private void validate(SaveMainHeaderDto dto) {
        if (dto == null || dto.getTitle() == null || dto.getTitle().isBlank()) {
            throw new RuntimeException("title is required");
        }
        if (dto.getUrlString() == null || dto.getUrlString().isBlank()) {
            throw new RuntimeException("urlString is required");
        }
    }

    private MainHeaderDto toDto(MainHeader header) {
        return MainHeaderDto.builder()
                .id(header.getId())
                .title(header.getTitle())
                .urlString(header.getUrlString())
                .isActive(header.getIsActive())
                .displayOrder(header.getDisplayOrder())
                .build();
    }
}
