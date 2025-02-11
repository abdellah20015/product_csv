import pandas as pd
import random
from faker import Faker
from datetime import datetime, timedelta

# Initialisation du générateur de données fictives
faker = Faker('fr_FR')

# Liste de catégories populaires au Maroc
categories = ["Électronique", "Vêtements", "Alimentation", "Cosmétique", "Électroménager", "Automobile"]

# Liste de régions marocaines
regions = ["Casablanca", "Rabat", "Marrakech", "Fès", "Tanger", "Agadir", "Meknès", "Oujda"]

# Nombre de produits à générer
N = 100

# Générer une date aléatoire dans une plage spécifique
def generate_random_date():
    # Date de début : 1er janvier 2023
    start_date = datetime(2023, 1, 1)
    # Date de fin : date actuelle
    end_date = datetime.now()

    # Calculer un temps aléatoire entre start et end
    time_between_dates = end_date - start_date
    days_between_dates = time_between_dates.days
    random_number_of_days = random.randrange(days_between_dates)
    random_date = start_date + timedelta(days=random_number_of_days)

    # Formater comme une chaîne ISO 8601 avec nanoseconde
    return random_date.isoformat() + 'Z'

# Générer les produits dynamiques
products = []
for _ in range(N):
    product = {
        "id": faker.uuid4(),
        "name": faker.word().capitalize() + " " + random.choice(["Premium", "Eco", "Deluxe", "Classique"]),
        "quantity": random.randint(1, 100),
        "price": round(random.uniform(10, 5000), 2),
        "category": random.choice(categories),
        "client": faker.company(),
        "region": random.choice(regions),
        "created_date": generate_random_date()
    }
    products.append(product)

# Convertir en DataFrame Pandas
df = pd.DataFrame(products)

# Sauvegarde en fichier CSV avec UTF-8
df.to_csv("products.csv", index=False, encoding="utf-8")

print("Le fichier 'products.csv' a été généré avec succès !")
