package dslab.transfer;

import dslab.util.Config;

import java.util.HashMap;
import java.util.Map;

public class DomainRegistry {

  private final Map<String, String> domainMap = new HashMap<>();

  public DomainRegistry(String configFilePath) {
    Config config = new Config(configFilePath);

    for (String key : config.listKeys()) {
      if (key.contains(".")) {
        domainMap.put(key, config.getString(key));
      }
    }
  }

  public String lookup(String domain) {
    return domainMap.get(domain);
  }
}
