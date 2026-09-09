package ds_bidding_system.account_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "user_accounts")
public class UserAccount {
    @Id
    @Column(name = "user_id", nullable = false, updatable = false)
    private String userId;
    @Column(nullable = false, length = 255)
    private String username;
    @Column(length = 254)
    private String email;

    protected UserAccount() {}
    public UserAccount(String userId, String username, String email) {
        this.userId = userId;
        this.username = username;
        this.email = email;
    }
    public String getUserId() { return userId; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
}
