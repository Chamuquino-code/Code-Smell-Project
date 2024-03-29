# Seyedali Shohadaalhosseini - Alishhde

# Feature envy smell
class Animals:
    def __init__(self, name, age, family, address):
        self.name = name
        self.age = age
        self.family = family
        self.address = address

class Dog(Animals):
    def __init__(self, name, age, family, address):
        super().__init__(name, age, family, address)
    
    def creatID(self):
        return str(self.family) + str(self.name) + str(self.age)

    def __str__(self) -> str:
        return f"Its name is {self.name}"

# Feature envy solution
class Animals:
    def __init__(self, name, age, family, address):
        self.name = name
        self.age = age
        self.family = family
        self.address = address
    
    def creatID(self):
        return str(self.family) + str(self.name) + str(self.age)
        
class Dog(Animals):
    def __init__(self, name, age, family, address):
        super().__init__(name, age, family, address)

    def __str__(self) -> str:
        return f"Its name is {self.name}"