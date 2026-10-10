package com.example.lostfound.config;

import com.example.lostfound.entity.*;
import com.example.lostfound.repository.*;
import com.example.lostfound.service.MatchingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("null")
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final ItemRepository itemRepository;
    private final PasswordEncoder passwordEncoder;
    private final MatchingService matchingService;

    @Value("${ADMIN_EMAIL:admin@college.com}")
    private String adminEmail;

    @Value("${ADMIN_PASSWORD:admin123}")
    private String adminPassword;

    @Value("${ADMIN_NAME:Campus Administrator}")
    private String adminName;

    @Override
    public void run(String... args) throws Exception {
        if (categoryRepository.count() == 0) {
            log.info("Initializing categories...");
            categoryRepository.save(Category.builder().name("Electronics").description("Laptops, phones, headphones, chargers, etc.").build());
            categoryRepository.save(Category.builder().name("Documents").description("ID cards, passports, certificates, notebooks.").build());
            categoryRepository.save(Category.builder().name("Accessories").description("Watches, jewelry, glasses, belts.").build());
            categoryRepository.save(Category.builder().name("Books").description("Textbooks, library books, novels.").build());
            categoryRepository.save(Category.builder().name("Bags").description("Backpacks, purses, laptop bags.").build());
            categoryRepository.save(Category.builder().name("Clothing").description("Jackets, sweaters, caps, scarves.").build());
            categoryRepository.save(Category.builder().name("Keys").description("Room keys, car keys, keychains.").build());
            categoryRepository.save(Category.builder().name("Other").description("Miscellaneous personal belongings.").build());
        }

        // Initialize admin account securely if missing
        if (!userRepository.existsByEmail(adminEmail)) {
            log.info("Creating initial administrator account ({})", adminEmail);
            userRepository.save(User.builder()
                    .name(adminName)
                    .email(adminEmail)
                    .password(passwordEncoder.encode(adminPassword))
                    .phone("+1-555-0199")
                    .role(Role.ADMIN)
                    .build());
        } else {
            log.info("Administrator account ({}) already exists in database.", adminEmail);
        }

        if (userRepository.count() <= 1) {
            log.info("Initializing seed users...");
            userRepository.save(User.builder()
                    .name("Campus Staff Officer")
                    .email("staff@college.com")
                    .password(passwordEncoder.encode("staff123"))
                    .phone("+1-555-0188")
                    .role(Role.STAFF)
                    .build());

            User student = userRepository.save(User.builder()
                    .name("Alice Smith")
                    .email("student@college.com")
                    .password(passwordEncoder.encode("student123"))
                    .phone("+1-555-0144")
                    .role(Role.USER)
                    .build());

            User student2 = userRepository.save(User.builder()
                    .name("John Doe")
                    .email("john@college.com")
                    .password(passwordEncoder.encode("student123"))
                    .phone("+1-555-0155")
                    .role(Role.USER)
                    .build());

            log.info("Initializing seed items...");
            Category electronics = categoryRepository.findByName("Electronics").orElse(null);
            Category bags = categoryRepository.findByName("Bags").orElse(null);

            // Lost report from Alice
            Item lostItem1 = itemRepository.save(Item.builder()
                    .title("Black Samsung Wireless Earbuds")
                    .description("Lost my black Samsung Galaxy Buds Pro inside charging case near university central library ground floor.")
                    .type(ItemType.LOST)
                    .category(electronics)
                    .location("Central Library")
                    .color("Black")
                    .brand("Samsung")
                    .dateLostOrFound(LocalDate.now().minusDays(2))
                    .status(ItemStatus.ACTIVE)
                    .reportedBy(student)
                    .build());

            // Found report from John
            itemRepository.save(Item.builder()
                    .title("Black Samsung Bluetooth Earbuds")
                    .description("Found black Samsung bluetooth wireless earbuds beside library entrance staircase near bench.")
                    .type(ItemType.FOUND)
                    .category(electronics)
                    .location("Library Entrance")
                    .color("Black")
                    .brand("Samsung")
                    .dateLostOrFound(LocalDate.now().minusDays(1))
                    .status(ItemStatus.ACTIVE)
                    .reportedBy(student2)
                    .build());

            // Lost report from John
            Item lostItem2 = itemRepository.save(Item.builder()
                    .title("Blue Dell Laptop Backpack")
                    .description("Blue nylon backpack containing college ID card, math textbook, and black Dell charger.")
                    .type(ItemType.LOST)
                    .category(bags)
                    .location("Science Block Hallway")
                    .color("Blue")
                    .brand("Dell")
                    .dateLostOrFound(LocalDate.now().minusDays(3))
                    .status(ItemStatus.ACTIVE)
                    .reportedBy(student2)
                    .build());

            // Found report from Alice
            itemRepository.save(Item.builder()
                    .title("Dark Blue Backpack with Dell Charger")
                    .description("Found dark blue backpack near Science Block Room 204. Contains textbook and charger.")
                    .type(ItemType.FOUND)
                    .category(bags)
                    .location("Science Block Room 204")
                    .color("Blue")
                    .brand("Dell")
                    .dateLostOrFound(LocalDate.now().minusDays(2))
                    .status(ItemStatus.ACTIVE)
                    .reportedBy(student)
                    .build());

            // Trigger AI matching for seed items
            log.info("Running initial AI matching process...");
            matchingService.processItemMatching(lostItem1);
            matchingService.processItemMatching(lostItem2);
            log.info("Sample data initialization complete!");
        }
    }
}
