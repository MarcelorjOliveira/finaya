package br.com.finaya.dto;

public class PixKeyRequest {
    private String type;     // EMAIL | telefone | EVP
    private String keyValue; // chave Pix

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getKeyValue() {
        return keyValue;
    }

    public void setKeyValue(String keyValue) {
        this.keyValue = keyValue;
    }
}
