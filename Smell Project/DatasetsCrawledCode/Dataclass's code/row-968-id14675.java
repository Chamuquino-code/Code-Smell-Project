@Entity
public class Customer1953 {


 @Id @GeneratedValue(strategy = GenerationType.AUTO) private long id;
 private String firstName;
 private String lastName;


 protected Customer1953() {}


 public Customer1953(String firstName, String lastName) {
 this.firstName = firstName;
 this.lastName = lastName;
	}


 @Override
 public String toString() {
 return String.format("Customer1953[id=%d, firstName='%s', lastName='%s']", id, firstName, lastName);
	}


}