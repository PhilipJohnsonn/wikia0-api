package com.scrapi.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.scrapi.service.PaginaWebService;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/scrap")
public class PaginaWebController {

    @Autowired
    private PaginaWebService paginaWebService;

    @GetMapping("/criaturas")
    public ResponseEntity<List<Map<String, Object>>> obtenerDatosCriaturas() {
        try {
            List<Map<String, Object>> data = paginaWebService.scrapear();
            return ResponseEntity.ok(data);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/drops")
    public ResponseEntity<List<Map<String, Object>>> obtenerDropsPorItem() throws Exception {
        try {
            return ResponseEntity.ok(paginaWebService.scrapearDropsPorItem());
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }


}
