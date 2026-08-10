package com.company.sportseq.service;

import com.company.sportseq.dto.WarehouseDTO;
import com.company.sportseq.entity.Warehouse;

import java.util.List;

public interface WarehouseService {

    List<Warehouse> list();

    void add(WarehouseDTO dto);

    void update(WarehouseDTO dto);

    void remove(Long id);
}
