package dslab.mailbox;

import dslab.util.Config;

public class UserRepository {
  private final Config config;
  private final Config userConfig;

  public UserRepository(Config config) {
    this.config = config;
    this.userConfig = new Config(config.getString("users.config"));
  }

  public boolean authenticate(String username, String password) {
    return this.userConfig.containsKey(username) &&
        userConfig.getString(username).equals(password);
  }

  public boolean userExists(String username) {
    return this.userConfig.containsKey(username);
  }


  public String getDomain() {
    return config.getString("domain");
  }


}
