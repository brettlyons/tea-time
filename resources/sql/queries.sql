-- name: create-user!
-- creates a new user record
INSERT INTO users
(id, first_name, last_name, email, pass)
VALUES (:id, :first_name, :last_name, :email, :pass)

-- name: update-user!
-- update an existing user record
UPDATE users
SET first_name = :first_name, last_name = :last_name, email = :email
WHERE id = :id

-- name: get-user
-- retrieve a user given the id.
SELECT * FROM users
WHERE id = :id

-- name: delete-user!
-- delete a user given the id
DELETE FROM users
WHERE id = :id

-- name: get-teas-name
-- retrieve all of the teas from the db
SELECT * FROM teas

-- name: create-tea!
-- put a new tea into the db
INSERT INTO teas
(name)
VALUES (:tea)

--name: update-tea!
-- update tea
UPDATE teas
SET name = :newname
WHERE id = :id

--name: delete-tea!
-- remove a tea from the database
DELETE FROM teas
WHERE name = :name
