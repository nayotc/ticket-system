package ticketsystem.DTO;

import java.time.LocalDate;

public class MyAccountDTO {

    private Long id;
    private String email;
    private String fullName;
    private String phone;
    private LocalDate birthDate;

    public MyAccountDTO() {
    }

    public MyAccountDTO(Long id, String email, String fullName, String phone, LocalDate birthDate) {
        this.id = id;
        this.email = email;
        this.fullName = fullName;
        this.phone = phone;
        this.birthDate = birthDate;
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getFullName() {
        return fullName;
    }

    public String getPhone() {
        return phone;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }
}