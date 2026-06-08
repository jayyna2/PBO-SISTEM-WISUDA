package model;

public abstract class User {
    protected String username;
    protected String password;
    protected String nama;
    protected String role;

    public User(String username, String password, String nama, String role) {
        this.username = username;
        this.password = password;
        this.nama = nama;
        this.role = role;
    }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getNama() { return nama; }
    public void setNama(String nama) { this.nama = nama; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public abstract String toJson();
}
