@Entity
public class Customer230 {


 @Id @GeneratedValue(strategy = GenerationType.AUTO) private long id;
 private String firstName;
 private String lastName;


 protected Customer230() {}


 public Customer230(String firstName, String lastName) {
 this.firstName = firstName;
 this.lastName = lastName;
	}


 @Override
 public String toString() {
 return String.format("Customer230[id=%d, firstName='%s', lastName='%s']", id, firstName, lastName);
	}


}