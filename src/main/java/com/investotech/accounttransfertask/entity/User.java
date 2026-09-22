@Entity
@Table(name = "user")
public class User {
    @Id
    private UUID id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "api_key_hash", nullable = false, unique = true, length = 64)
    private String apiKeyHash;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
