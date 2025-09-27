## 1. Filter Chain

<img width="1100" height="610" alt="filterChain" src="https://github.com/user-attachments/assets/4be29c58-f918-4c32-80dd-0654bea564a7" />

---

## 2. Session ID

When a user logs in, the server creates (or associates) an HTTP session and gives it a **session ID**. That session remains valid until the user explicitly logs out (or the session expires). During that period, you don’t need to reauthenticate on every request — the session ID is what ties your requests to your login state.

If you wish to return the session ID in a response, you could write something like:

```java
public String getSessionId(HttpServletRequest request) {
    return request.getSession().getId();
}
```

* The session ID will **change** if you log out and then log in again (a new session is created).
* Spring Security provides protections against session fixation (i.e. ensuring attackers can’t reuse an old session) by default.
* You can choose how Spring Security handles sessions: e.g. always create, only when needed, never use sessions, or be stateless.

---

## 3. Customizing / Dynamically Changing Username & Password

* Setting username and password in application.propertise

```
spring.security.user.name=omar
spring.security.user.password=123
```

* During authentication, the `UsernamePasswordAuthenticationFilter` checks whether a username/password is defined in your application’s properties.
* If not, the system should create or use default credentials

---

## 4. CSRF


### What is CSRF

* **CSRF (Cross-Site Request Forgery)** is an attack where a malicious site tricks a user’s browser 
(already logged into your site) to make unwanted requests (e.g., POST, PUT, DELETE) to your site as if from the user.


### How Spring Security’s CSRF Filter Works

1. **Default Behavior**
   Spring Security enables CSRF protection by default.

2. **When it applies**
   For state-changing HTTP methods (POST, PUT, DELETE, PATCH, etc.), Spring’s CSRF filter expects a valid CSRF token to be present 
(in a request header or parameter). If no valid token is present, the filter will reject the request 
(e.g. respond with 403 Forbidden, or 401/unauthorized depending on configuration).

3. **How tokens are stored / validated**

   * By default, Spring uses `HttpSessionCsrfTokenRepository`, storing the expected CSRF token in the user’s HTTP session.
   * The CSRF token is exposed as a request attribute, typically under name `"_csrf"`.
   * On each unsafe request, Spring compares the token in the request (header or request parameter) against the stored token. 
	If they don’t match, the request is rejected.


### Example: retrieving the CSRF token in a controller

You can get the CSRF token from the current request, for example:

```java
public CsrfToken getCsrfToken(HttpServletRequest request) {
    Object tokenObj = request.getAttribute("_csrf");
    if (tokenObj instanceof CsrfToken) {
        return (CsrfToken) tokenObj;
    }
    return null;
}
```

Then you can send parts of it (e.g. the token’s value) back to the client, so the client (JavaScript, frontend) can include it in subsequent POST/PUT/DELETE requests.


### Special Cases & Alternatives

* **Stateless APIs / token-based APIs**
  
  If your backend is a stateless REST API (e.g. using JWT tokens, no HTTP session), CSRF protection often doesn’t apply (or is disabled).
  
* **If you regenerate session every request (or disable session state)**
  
  If your application forces a brand new session on every request (i.e. you don’t maintain a session across requests), 
  CSRF tokens tied to sessions won’t persist — thus CSRF checks break. 
  In that case, CSRF protection via session token doesn’t make sense, so it may be disabled.

* **SameSite cookie policy**
  Setting the `SameSite` attribute (e.g. `SameSite=Strict` or `SameSite=Lax`) on your session cookie can helps reduce CSRF by blocking cookies on cross-site requests,
 But it’s only extra protection, not a replacement for CSRF tokens.

---

## 5. Spring Security Configuration

### 1. **Define a configuration class**
   Annotate your class with:

   ```java
   @Configuration
   @EnableWebSecurity
   public class SecurityConfig {
       // beans and security setup go here
   }
   ```

   * `@Configuration` tells Spring that this class contains bean definitions.
   * `@EnableWebSecurity` enables Spring Security’s web security support and tells Spring not to use its default configuration, but to use what you define.


### 2. **Customize the security filter chain** 

you configure security by defining a `SecurityFilterChain` bean.

   ```java
   @Bean
   public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
       // configure your security rules, authentication, etc.
       return http.build();
   }
   ```

   The `HttpSecurity` object allows you to customize how security behaves 
   (which endpoints require authentication, what kind of login mechanism you use, CSRF, session management, etc.).
   When you call `http.build()`, Spring builds and returns the configured `SecurityFilterChain`.


2. **Customize the security filter chain**
   Starting from Spring Security 5.4 (and encouraged since deprecation of `WebSecurityConfigurerAdapter`), you configure security by defining a `SecurityFilterChain` bean. ([Home][1])

   ```java
   @Bean
   public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
       // configure your security rules, authentication, etc.
       return http.build();
   }
   ```

   The `HttpSecurity` object allows you to customize how security behaves (which endpoints require authentication, what kind of login mechanism you use, CSRF, session management, etc.). When you call `http.build()`, Spring builds and returns the configured `SecurityFilterChain`.

3. **Make sure your configuration is “applied”**
   It’s not enough just to return `http.build()`—you have to *configure* `http` properly before building. For example:

   ```java

	@Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception {
        httpSecurity.csrf(AbstractHttpConfigurer::disable)
   				.cors(Customizer.withDefaults())
                .authorizeHttpRequests(
                        authorizationManagerRequestMatcherRegistry ->
                                authorizationManagerRequestMatcherRegistry.requestMatchers("/login", "/images/**").permitAll() // just allows those
                                        .anyRequest().authenticated() // else authenticate every request
                ).httpBasic(Customizer.withDefaults())

   				// make the session statless 
				// problem with this you can't login from your login form in browser because every request you have to pass
				// credintials and when it goes from login form to resource it need the credintials again
				// but in postman it works fine
				// if you need to work correctly with browser -> disable your your form login -> it will show a pop-up to enter username and password
                .sessionManagement(httpSecuritySessionManagementConfigurer ->
                        httpSecuritySessionManagementConfigurer.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

				// it enables login form in browser but in postman it response a html
   				// if you are using postman you can disable it
   				// so to enable authentication from postman you need to add this .httpBasic(Customizer.withDefaults())
   				// .formLogin(Customizer.withDefaults()); 

   
        return httpSecurity.build();
    }
   
   ```

   Without those configurations on `http`, the resulting `SecurityFilterChain` may do nothing or default behavior which might not match your intentions.


	### you can do that in csrf
	
	``` java
	@Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception {
 
		Customizer<CsrfConfigurer<HttpSecurity>> csrfFilterCustomizer = new Customizer<>() {
			@Override
			public void customize(CsrfConfigurer<HttpSecurity> httpSecurityCsrfConfigurer) {
				httpSecurityCsrfConfigurer.disable();
			}
		};
		
		httpSecurity.csrf(csrfFilterCustomizer);

	}
	```

---

## From Static Credentials → Database-Backed Authentication

### What we had so far

* Up to now, you could log in using a **single username + password** stored in `application.properties`.
* This works for a single user, but it’s not scalable. You want to switch to **database storage** so you can have many users, roles, etc.

### What changes with database authentication

When you submit username and password:

1. The request arrives at the authentication filter (e.g. the `UsernamePasswordAuthenticationFilter`) as an **unauthenticated** `Authentication` object (i.e. `isAuthenticated() == false`).
2. That object is passed to the **AuthenticationManager (or chain of AuthenticationProviders)**.
3. `DaoAuthenticationProvider` calls your `UserDetailsService` to fetch user data.
4. An `AuthenticationProvider` checks the credentials (against the database, in this new setup).
5. If valid, it returns a fully **authenticated** `Authentication` object (with authorities, principal, etc.).
6. Spring Security then places that `Authentication` into the `SecurityContext`, and the user is considered “logged in.”
7. Future requests use that context (via session or token) so the user stays authenticated.


<img width="1100" height="481" alt="securityFlow" src="https://github.com/user-attachments/assets/617d8dfa-50b7-475a-bfe0-d1b23cce9853" />

---

## Customizing the Authentication Provider in Spring Security

### 1. Why customize?

By default, Spring Security uses its own **AuthenticationProvider** internally to handle username and password authentication.
If you want to connect to a **database** (or even an external service), you’ll often need to replace this with your **own authentication provider** setup.


### 2. What is an AuthenticationProvider?

* It’s an **interface** in Spring Security.
* There are multiple implementations available. One common implementation is **`DaoAuthenticationProvider`**, which retrieves user details from a database (via `UserDetailsService`) and compares passwords (via a `PasswordEncoder`).
* To use it, you just need to configure two things:

  1. **PasswordEncoder** (e.g. BCrypt)
  2. **UserDetailsService** (your custom implementation for fetching users)


### 3. Defining the Beans

```java
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
}

@Autowired
private CustomUserDetailsService userDetailsService;

@Bean
public AuthenticationProvider authenticationProvider() {
    DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
    authProvider.setPasswordEncoder(passwordEncoder());
    authProvider.setUserDetailsService(userDetailsService);

    return authProvider;
}
```

Here:

* `passwordEncoder()` → ensures user passwords are hashed & verified securely.
* `authenticationProvider()` → creates a `DaoAuthenticationProvider` configured with your encoder + custom user details service.

This replaces the default provider with **your own customized authentication flow**.


### 4. Implementing `UserDetailsService`

`UserDetailsService` is also an interface. You must implement it to tell Spring how to fetch users from your data source.

Example:

```java
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("Email not found: " + username));

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())
                .password(user.getPassword())
                .roles("USER") // you can fetch roles dynamically from DB as well
                .build();
    }
}
```

Here:

* Spring will call `loadUserByUsername()` when someone tries to log in.
* You fetch the user from the database.
* If found, you return a Spring Security `UserDetails` object (which includes username, password, and roles).
* If not found, throw `UsernameNotFoundException`.


### 5. Authentication Flow (with custom provider)

1. User submits username & password.
2. Spring Security creates an **unauthenticated Authentication object**.
3. It calls your configured `AuthenticationProvider` (`DaoAuthenticationProvider`).
4. That provider calls your `CustomUserDetailsService.loadUserByUsername()`.
5. The returned `UserDetails` is checked against the submitted password using the `PasswordEncoder`.
6. If valid → an **authenticated Authentication object** is returned and stored in the `SecurityContext`.
7. If invalid → an exception is thrown, and authentication fails.

---

```java
@Bean
public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
    return configuration.getAuthenticationManager();
}
```

* This exposes Spring Security’s internal `AuthenticationManager` as a bean, so you can `@Autowired` it elsewhere (e.g. in custom filters or services).
* `AuthenticationConfiguration` already knows about your configured `AuthenticationProvider`s, `UserDetailsService`, and `PasswordEncoder`, so calling `getAuthenticationManager()` gives you the fully composed manager.
* Without this bean, you may not have access to the `AuthenticationManager` in parts of your application outside the usual security filters.

---






