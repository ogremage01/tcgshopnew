package com.shop.card.metadata.entity;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "mtg_set_infos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MtgSetInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    //----------------------------------------------------
    // 외부 API에서 온 데이터
    //----------------------------------------------------

    private String setCode;
    
    private String name;
    
    @Column(name = "name_k")
    @JsonProperty("name_k")
    private String nameK;

    @Column(name = "set_type")
    private String type;


    @Column(name = "release_date")
    private LocalDateTime releaseDate;

    //----------------------------------------------------
    // 외부 API에서 온 데이터가 아니므로 직접 저장한다.
    //----------------------------------------------------

    @CreatedDate
    private LocalDateTime createdAt;

}
