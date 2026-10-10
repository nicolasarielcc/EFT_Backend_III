package cl.duoc.banco.bff.cuentas.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import cl.duoc.banco.bff.cuentas.entity.Cuenta;

public interface CuentaRepository extends JpaRepository<Cuenta, Long> {
}
