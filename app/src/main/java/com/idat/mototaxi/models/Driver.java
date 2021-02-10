package com.idat.mototaxi.models;

public class Driver {

    String id;
    String name;
    String email;
    String marca;
    String placa;
    String image;

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public Driver() {}

    public Driver(String id, String name, String email, String marca, String placa) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.marca = marca;
        this.placa = placa;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getMarca() {
        return marca;
    }

    public void setMarca(String modelo) {
        this.marca = modelo;
    }

    public String getPlaca() {
        return placa;
    }

    public void setPlaca(String placa) {
        this.placa = placa;
    }
}
