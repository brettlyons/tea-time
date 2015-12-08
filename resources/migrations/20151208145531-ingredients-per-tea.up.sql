CREATE TABLE ingredients_per_tea
(id serial PRIMARY KEY,
tea INT REFERENCES teas,
tea_ingredients INT REFERENCES tea_ingredients);
