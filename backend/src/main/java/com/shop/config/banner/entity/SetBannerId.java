package com.shop.config.banner.entity;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class SetBannerId implements Serializable {

    @Column(name = "game", nullable = false, length = 50)
    private String game;

    @Column(name = "banner_id", nullable = false, length = 100)
    private String bannerId;
}
