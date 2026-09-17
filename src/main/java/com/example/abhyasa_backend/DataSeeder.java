package com.example.abhyasa_backend;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class DataSeeder implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ServiceRepository serviceRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public void run(String... args) {
        seedAdminUser();
        seedDefaultServices();
        seedDoctors();
    }

    private void seedAdminUser() {
        String adminEmail = "gnanesh1561@gmail.com";

        if (!userRepository.existsByEmail(adminEmail)) {
            User admin = new User();
            admin.setEmail(adminEmail);
            admin.setFirstName("Gnanesh");
            admin.setLastName("Admin");
            admin.setPassword(passwordEncoder.encode("Admin@1561"));
            admin.setRole("ADMIN");
            userRepository.save(admin);
            System.out.println("✅ Default ADMIN user created: " + adminEmail);
        } else {
            // Ensure existing user has ADMIN role
            userRepository.findByEmail(adminEmail).ifPresent(user -> {
                if (!"ADMIN".equals(user.getRole())) {
                    user.setRole("ADMIN");
                    userRepository.save(user);
                    System.out.println("✅ Upgraded existing user to ADMIN: " + adminEmail);
                }
            });
        }
    }

    private void seedDefaultServices() {
        if (serviceRepository.count() > 0) {
            System.out.println("ℹ️ Services already exist. Skipping seed.");
            return;
        }

        String[][] services = {
            // {name, description, category, color, iconName, displayOrder}
            {"Speech Therapy",
             "Helping children find their voice — from first words to confident, everyday conversations. Our speech-language pathologists use play-based, structured methods and AAC devices when needed.",
             "rehabilitation", "teal", "message-circle", "1"},

            {"Occupational Therapy",
             "Building fine motor skills, sensory processing, and independence in daily activities. We help children develop the skills they need for writing, dressing, feeding, play, and classroom participation.",
             "rehabilitation", "coral", "hand", "2"},

            {"Pediatric Physiotherapy",
             "Strengthening gross motor skills, coordination, balance, and movement confidence through therapeutic exercises and play.",
             "rehabilitation", "sky", "activity", "3"},

            {"Behavioural Therapy",
             "Evidence-based ABA methods and positive behaviour support to help children develop social skills, manage challenging behaviours, build communication, and become more independent.",
             "rehabilitation", "lavender", "users", "4"},

            {"Neuro Developmental & Behavioural Pediatrics",
             "Comprehensive assessments and medical management for developmental and behavioural conditions.",
             "medical", "teal", "zap", "5"},

            {"Autism Spectrum Disorder",
             "Structured support for social communication, sensory needs, and daily routines across all ages.",
             "medical", "coral", "smile", "6"},

            {"Early Intervention",
             "Timely support when developmental concerns are first noticed — no diagnosis required.",
             "rehabilitation", "golden", "clock", "7"},

            {"Sensory Integration",
             "For children over- or under-sensitive to sounds, textures, and movement — learning to self-regulate.",
             "rehabilitation", "lavender", "radio", "8"},

            {"Psychological Assessments",
             "Detailed evaluations for learning, behaviour, and emotional well-being guiding the right therapy plan.",
             "rehabilitation", "sky", "file-text", "9"},

            {"Remedial Education",
             "Individualised academic support using multi-sensory teaching methods for children with learning difficulties.",
             "rehabilitation", "teal", "book-open", "10"},

            {"Adolescence Guidance & Counselling",
             "Supporting teenagers through emotional, social, and academic challenges in a safe space.",
             "support", "coral", "user", "11"},

            {"Parents Training Program",
             "Empowering parents with strategies and skills to support their child's development at home.",
             "support", "golden", "calendar", "12"},

            {"Family Counselling",
             "Helping families navigate the emotional journey of raising a child with special needs.",
             "support", "lavender", "home", "13"},

            {"High Risk New Born Follow Up",
             "Monitoring and supporting babies who had a complicated birth or NICU stay.",
             "support", "teal", "star", "14"},

            {"PICU Follow Up",
             "Continued care for children recovering after a paediatric intensive care unit stay.",
             "support", "sky", "heart", "15"},

            {"Home Based Intervention",
             "Therapy delivered in the comfort of your home for children who need it.",
             "support", "coral", "home", "16"},

            {"General Screening",
             "Initial assessment session for parents who are unsure which service their child needs. Our specialists evaluate and recommend the right therapy path.",
             "medical", "golden", "search", "0"},
        };

        for (String[] s : services) {
            Service service = new Service();
            service.setName(s[0]);
            service.setDescription(s[1]);
            service.setCategory(s[2]);
            service.setColor(s[3]);
            service.setIconName(s[4]);
            service.setDisplayOrder(Integer.parseInt(s[5]));
            service.setDurationMinutes(30);
            service.setIsActive(true);
            serviceRepository.save(service);
        }

        System.out.println("✅ Seeded " + services.length + " default services.");
    }

    private void seedDoctors() {
        if (doctorRepository.count() > 0) {
            System.out.println("ℹ️ Doctors already exist. Skipping seed.");
            return;
        }

        // Make sure services are loaded
        List<Service> allServices = serviceRepository.findAll();
        if (allServices.isEmpty()) {
            System.out.println("⚠️ No services found. Cannot assign services to doctors.");
            return;
        }

        // Helper to find services by name
        System.out.println("🩺 Seeding 8 doctors...");

        // 1. Dr. Priya Sharma — Founder & Clinical Director
        createDoctor(
            "Dr. Priya Sharma", "priya.sharma@abhyasacdc.in", "+918106843001",
            "Founder & Clinical Director", "MBBS, MD Pediatrics", "Leadership",
            "15+ years",
            "With over 15 years of experience in pediatric care and developmental medicine, Dr. Priya founded Abhyasa to bridge the gap between recognising a developmental concern and getting the right help.",
            "Dr. Priya believes in a family-centred approach where parents are equal partners in therapy. She personally oversees every child's initial assessment and coordinates treatment plans across all disciplines at Abhyasa.",
            "MBBS — Andhra Medical College | MD Pediatrics — NIMHANS, Bangalore",
            "Developmental Pediatrics, Early Intervention, Multidisciplinary Care",
            "English, Telugu, Hindi",
            allServices,
            new String[]{"General Screening", "Neuro Developmental & Behavioural Pediatrics", "Early Intervention", "High Risk New Born Follow Up", "PICU Follow Up"}
        );

        // 2. Kavitha Reddy — Senior Speech-Language Pathologist
        createDoctor(
            "Kavitha Reddy", "kavitha.reddy@abhyasacdc.in", "+918106843002",
            "Senior Speech-Language Pathologist", "M.Sc SLP, RCI Registered", "Speech & Language",
            "8+ years",
            "Specialising in early communication intervention, Kavitha helps children find their voice — from first words to confident conversations.",
            "Kavitha uses play-based methods to build communication skills naturally. She works closely with parents to carry over strategies at home, ensuring progress extends beyond therapy sessions.",
            "M.Sc Speech-Language Pathology — AIISH, Mysore | RCI Registration No. A12345",
            "Early Language Intervention, Articulation Therapy, AAC Devices",
            "English, Telugu",
            allServices,
            new String[]{"Speech Therapy", "Early Intervention", "Autism Spectrum Disorder", "General Screening"}
        );

        // 3. Rajesh Kumar — Occupational Therapist
        createDoctor(
            "Rajesh Kumar", "rajesh.kumar@abhyasacdc.in", "+918106843003",
            "Occupational Therapist", "BOT, Sensory Integration Certified", "Occupational Therapy",
            "6+ years",
            "Rajesh focuses on sensory processing, fine motor skills, and daily living activities — helping children participate fully in school and life.",
            "Rajesh creates structured yet fun therapy sessions that improve a child's ability to engage with their surroundings, build motor confidence, and become more independent in daily routines.",
            "BOT — Manipal Academy of Higher Education | Sensory Integration Certification — USC/WPS",
            "Sensory Processing, Fine Motor Development, Handwriting Improvement",
            "English, Telugu, Hindi",
            allServices,
            new String[]{"Occupational Therapy", "Sensory Integration", "Early Intervention", "Home Based Intervention"}
        );

        // 4. Ananya Iyer — Child Psychologist
        createDoctor(
            "Ananya Iyer", "ananya.iyer@abhyasacdc.in", "+918106843004",
            "Child Psychologist", "M.Phil Clinical Psychology", "Psychology",
            "7+ years",
            "Ananya works with children facing behavioural challenges, anxiety, and learning difficulties — always in partnership with families.",
            "Ananya combines clinical assessments with compassionate counselling. She helps families understand their child's behaviour through a strengths-based lens and provides practical strategies for home and school.",
            "M.Phil Clinical Psychology — NIMHANS, Bangalore | B.A Psychology — Loyola College, Chennai",
            "Behavioral Assessment, CBT for Children, Parent Counselling",
            "English, Telugu, Tamil",
            allServices,
            new String[]{"Psychological Assessments", "Adolescence Guidance & Counselling", "Family Counselling", "Parents Training Program", "General Screening"}
        );

        // 5. Meena Lakshmi — Special Educator
        createDoctor(
            "Meena Lakshmi", "meena.lakshmi@abhyasacdc.in", "+918106843005",
            "Special Educator", "B.Ed Special Education", "Special Education",
            "5+ years",
            "Meena designs individualised learning plans that help children with learning disabilities thrive in academic and social settings.",
            "Meena works one-on-one and in small groups, using multi-sensory teaching techniques to make academic content accessible and enjoyable for children with diverse learning needs.",
            "B.Ed Special Education — Osmania University | Diploma in Learning Disabilities",
            "IEP Development, Reading & Writing Support, Inclusive Education",
            "English, Telugu",
            allServices,
            new String[]{"Remedial Education", "Early Intervention", "Autism Spectrum Disorder"}
        );

        // 6. Arjun Rao — Behavioral Therapist
        createDoctor(
            "Arjun Rao", "arjun.rao@abhyasacdc.in", "+918106843006",
            "Behavioral Therapist", "M.Sc Applied Behavior Analysis", "Behavioral Therapy",
            "5+ years",
            "Arjun uses evidence-based behavioural strategies to help children develop social skills, manage emotions, and build independence.",
            "Arjun uses structured ABA methods combined with naturalistic teaching to help children learn new skills in real-world contexts — school, playground, and home.",
            "M.Sc Applied Behavior Analysis — University of Mumbai | BCBA Coursework Completed",
            "ABA Therapy, Social Skills Training, Positive Behavior Support",
            "English, Telugu, Hindi",
            allServices,
            new String[]{"Behavioural Therapy", "Autism Spectrum Disorder", "Home Based Intervention"}
        );

        // 7. Divya Nair — Occupational Therapist
        createDoctor(
            "Divya Nair", "divya.nair@abhyasacdc.in", "+918106843007",
            "Occupational Therapist", "BOT, Pediatric OT Certified", "Occupational Therapy",
            "4+ years",
            "Divya specialises in handwriting, visual-motor skills, and helping children with sensory sensitivities navigate everyday environments.",
            "Divya uses creative activities and adaptive tools to help children build hand strength, coordination, and confidence in writing, drawing, and self-care tasks.",
            "BOT — SRM Institute of Science and Technology | Pediatric OT Certification",
            "Handwriting Remediation, Visual-Motor Integration, Self-Care Skills",
            "English, Telugu, Malayalam",
            allServices,
            new String[]{"Occupational Therapy", "Sensory Integration", "Pediatric Physiotherapy"}
        );

        // 8. Vikram Prasad — Speech-Language Pathologist
        createDoctor(
            "Vikram Prasad", "vikram.prasad@abhyasacdc.in", "+918106843008",
            "Speech-Language Pathologist", "BASLP, RCI Registered", "Speech & Language",
            "4+ years",
            "Vikram works with children on articulation, fluency, and language comprehension — making communication a joyful part of every session.",
            "Vikram makes every session interactive and fun, using games, stories, and role-play to target speech goals. He believes communication should never feel like a chore for a child.",
            "BASLP — Dr. S.R. Chandrasekhar Institute, Bangalore | RCI Registration No. A67890",
            "Fluency Therapy, Articulation Disorders, Language Comprehension",
            "English, Telugu, Kannada",
            allServices,
            new String[]{"Speech Therapy", "Autism Spectrum Disorder", "Home Based Intervention"}
        );

        System.out.println("✅ Seeded 8 doctors with service assignments.");
    }

    /**
     * Creates a User (DOCTOR role), a Doctor profile, and assigns services.
     */
    private void createDoctor(
        String fullName, String email, String phone,
        String title, String badge, String department, String experience,
        String bio, String approach, String education,
        String specialties, String languages,
        List<Service> allServices, String[] serviceNames
    ) {
        // 1. Create or find User account
        User doctorUser;
        if (userRepository.existsByEmail(email)) {
            doctorUser = userRepository.findByEmail(email).get();
            doctorUser.setRole("DOCTOR");
            doctorUser = userRepository.save(doctorUser);
        } else {
            doctorUser = new User();
            doctorUser.setEmail(email);
            String[] parts = fullName.replace("Dr. ", "").trim().split(" ", 2);
            doctorUser.setFirstName(parts[0]);
            doctorUser.setLastName(parts.length > 1 ? parts[1] : "");
            doctorUser.setPhoneNumber(phone);
            doctorUser.setRole("DOCTOR");
            doctorUser.setPassword(passwordEncoder.encode("Doctor@123"));
            doctorUser = userRepository.save(doctorUser);
        }

        // 2. Create Doctor profile
        Doctor doctor = new Doctor();
        doctor.setUser(doctorUser);
        doctor.setFullName(fullName);
        doctor.setEmail(email);
        doctor.setPhoneNumber(phone);
        doctor.setTitle(title);
        doctor.setBadge(badge);
        doctor.setDepartment(department);
        doctor.setExperience(experience);
        doctor.setBio(bio);
        doctor.setApproach(approach);
        doctor.setEducation(education);
        doctor.setSpecialties(specialties);
        doctor.setLanguages(languages);
        doctor.setIsActive(true);

        // 3. Assign services
        List<Service> assignedServices = new ArrayList<>();
        for (String svcName : serviceNames) {
            for (Service s : allServices) {
                if (s.getName().equals(svcName)) {
                    assignedServices.add(s);
                    break;
                }
            }
        }
        doctor.setServices(assignedServices);

        doctorRepository.save(doctor);
        System.out.println("   ✅ " + fullName + " → " + assignedServices.size() + " services");
    }
}

