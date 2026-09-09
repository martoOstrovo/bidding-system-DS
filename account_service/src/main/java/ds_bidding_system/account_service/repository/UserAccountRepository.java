package ds_bidding_system.account_service.repository;

import ds_bidding_system.account_service.entity.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAccountRepository extends JpaRepository<UserAccount, String> {}
