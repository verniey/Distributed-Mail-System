package dslab.mail;

import java.util.List;

public class Email {
  private Long id;
  private String from;
  private List<String> to;
  private String data;
  private String subject;

  public Email() {
  }

  public Email(String from, List<String> to, String data, String subject) {
    this.from = from;
    this.to = to;
    this.data = data;
    this.subject = subject;
  }

  public Email(Long id, String from, List<String> to, String data, String subject) {
    this.id = id;
    this.from = from;
    this.to = to;
    this.data = data;
    this.subject = subject;
  }

  public Long getId() {
    return id;
  }


  public String getFrom() {
    return from;
  }

  public void setFrom(String from) {
    this.from = from;
  }

  public List<String> getTo() {
    return to;
  }

  public void setTo(List<String> to) {
    this.to = to;
  }

  public String getData() {
    return data;
  }

  public void setData(String data) {
    this.data = data;
  }

  public String getSubject() {
    return subject;
  }

}
