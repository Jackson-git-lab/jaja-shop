-- V1 : première migration (baseline).
-- À partir de là, c'est Flyway qui gère les tables de la base.
-- Chaque nouvelle table aura sa propre migration : V2, V3, etc.
-- Une migration déjà appliquée ne se modifie jamais.
-- Si on s'est trompé, on ajoute une nouvelle migration pour corriger.

SELECT 1 ;