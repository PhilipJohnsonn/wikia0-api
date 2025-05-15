//package com.scrapi.entity;
//
//import java.util.List;
//
//import jakarta.persistence.CascadeType;
//import jakarta.persistence.Entity;
//import jakarta.persistence.GeneratedValue;
//import jakarta.persistence.GenerationType;
//import jakarta.persistence.Id;
//import jakarta.persistence.JoinColumn;
//import jakarta.persistence.OneToMany;
//import lombok.Data;
//
//@Entity
//@Data
//public class Criatura {
//
//	@Id
//	@GeneratedValue(strategy = GenerationType.IDENTITY)
//	private Long id;
//
//	private String nombre;
//	private String imageUrl; // extraída de la celda Body
//	private Integer hp;
//	private Integer exp;
//	private Integer oro;
//	private Integer respawnMin; // en segundos
//	private Integer respawnMax; // en segundos
//
//	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
//	@JoinColumn(name = "criatura_id")
//	private List<Drop> drops;
//}
