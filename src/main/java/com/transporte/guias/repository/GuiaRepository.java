package com.transporte.guias.repository;

import com.transporte.guias.model.Guia;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class GuiaRepository {
    private final Map<String, Guia> guias = new ConcurrentHashMap<>();
    
    public Guia save(Guia guia) {
        if (guia.getId() == null) {
            guia.setId(UUID.randomUUID().toString());  
        }
        guias.put(guia.getId(), guia);
        return guia;
    }
    
    public Optional<Guia> findById(String id) {
        return Optional.ofNullable(guias.get(id));
    }
    
    public List<Guia> findAll() {
        return new ArrayList<>(guias.values());
    }
    
    public List<Guia> findByTransportistaAndFecha(String transportista, LocalDate fecha) {
        return guias.values().stream()
                .filter(g -> g.getTransportista().equalsIgnoreCase(transportista))
                .filter(g -> g.getFecha().equals(fecha))
                .toList();  
    }
    
    public void deleteById(String id) {
        guias.remove(id);
    }
    
    public boolean existsById(String id) {
        return guias.containsKey(id);
    }
}