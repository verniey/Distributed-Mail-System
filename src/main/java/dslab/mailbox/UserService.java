package dslab.mailbox;


public class UserService {

  private final UserRepository userRepository;

  public UserService(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  public boolean authenticate(String username, String password) {
    return userRepository.authenticate(username, password);
  }

  public boolean userExists(String username) {
    return userRepository.userExists(username);
  }

  public String getDomain() {
    return userRepository.getDomain();
  }
}
