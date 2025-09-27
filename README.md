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

---



