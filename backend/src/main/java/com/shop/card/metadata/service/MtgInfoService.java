package com.shop.card.metadata.service;

import java.util.List;

import com.shop.card.metadata.dto.MtgSetInfoDto;

public interface MtgInfoService {

    public List<MtgSetInfoDto> getMtgSetInfoList();

    public MtgSetInfoDto getMtgSetInfo(String setCode);

}
