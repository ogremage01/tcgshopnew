package com.shop.card.metadata.service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.shop.card.metadata.dto.MtgSetInfoDto;
import com.shop.card.metadata.entity.MtgSetInfo;
import com.shop.card.metadata.repository.MtgSetInfoRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class MtgInfoServiceImpl implements MtgInfoService {

    private final MtgSetInfoRepository mtgSetInfoRepository;

    // 무시할 세트 타입
    private static final List<String> IGNORE_TYPES = Arrays.asList("token", "tokens");

    @Override
    public List<MtgSetInfoDto> getMtgSetInfoList() {
        List<MtgSetInfo> mtgSetInfoList = mtgSetInfoRepository.findAllByOrderByReleaseDateDescOnlyCardSets();
        return convertToDto(mtgSetInfoList);
    }

    @Override
    public MtgSetInfoDto getMtgSetInfo(String setCode) {
        MtgSetInfo mtgSetInfo = mtgSetInfoRepository.findBySetCode(setCode).orElse(null);
        if (mtgSetInfo == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "세트를 찾을 수 없습니다.");
        }
        return toDto(mtgSetInfo);
    }

    private List<MtgSetInfoDto> convertToDto(List<MtgSetInfo> mtgSetInfoList) {
        return mtgSetInfoList.stream()
                // 무시할 세트 타입 제외
                .filter(entity -> !IGNORE_TYPES.contains(entity.getType().toLowerCase()))
                // this::toDto 는 toDto 메서드를 호출하는 람다식을 의미한다.
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    private MtgSetInfoDto toDto(MtgSetInfo entity) {
        MtgSetInfoDto dto = new MtgSetInfoDto();
        dto.setSetCode(entity.getSetCode());
        dto.setName(entity.getName());
        dto.setNameK(entity.getNameK());
        dto.setType(entity.getType());
        dto.setReleaseDate(entity.getReleaseDate());
        return dto;
    }

}
