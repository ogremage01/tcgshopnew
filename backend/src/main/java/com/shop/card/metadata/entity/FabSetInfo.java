package com.shop.card.metadata.entity;

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
@Table(name = "fab_set_infos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FabSetInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "set_code")
    private String setCode;
    @Column(name = "name")
    private String name;
    @Column(name = "porder")
    private Long porder;
    @Column(name = "category")
    private String category;

}
