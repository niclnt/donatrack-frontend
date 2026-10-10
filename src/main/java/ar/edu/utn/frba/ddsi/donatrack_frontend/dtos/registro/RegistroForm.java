package ar.edu.utn.frba.ddsi.donatrack_frontend.dtos.registro;

// Datos del formulario de registro. Cada tipo de cuenta usa solo algunos campos.
// Edad y telefono son String para validarlos nosotros con un mensaje claro.
public class RegistroForm {

    // Cuenta (todos)
    private String mail;
    private String password;
    private String confirmacion;

    // Persona humana
    private String nombre;
    private String edad;
    private String tipoDoc = "DNI";
    private String documento;
    private String genero;
    private String medioPreferido = "EMAIL";

    // Persona juridica y entidad beneficiaria
    private String razonSocial;

    // Persona juridica
    private String tipoJuridico = "EMPRESA";
    private String rubro;

    // Persona humana y entidad beneficiaria
    private String direccion;
    private String telefono;

    // Entidad beneficiaria
    private String localidad;
    private String ciudad;
    private String provincia = "Buenos Aires";

    public String getMail() { return mail; }
    public void setMail(String mail) { this.mail = mail; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getConfirmacion() { return confirmacion; }
    public void setConfirmacion(String confirmacion) { this.confirmacion = confirmacion; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getEdad() { return edad; }
    public void setEdad(String edad) { this.edad = edad; }
    public String getTipoDoc() { return tipoDoc; }
    public void setTipoDoc(String tipoDoc) { this.tipoDoc = tipoDoc; }
    public String getDocumento() { return documento; }
    public void setDocumento(String documento) { this.documento = documento; }
    public String getGenero() { return genero; }
    public void setGenero(String genero) { this.genero = genero; }
    public String getMedioPreferido() { return medioPreferido; }
    public void setMedioPreferido(String medioPreferido) { this.medioPreferido = medioPreferido; }
    public String getRazonSocial() { return razonSocial; }
    public void setRazonSocial(String razonSocial) { this.razonSocial = razonSocial; }
    public String getTipoJuridico() { return tipoJuridico; }
    public void setTipoJuridico(String tipoJuridico) { this.tipoJuridico = tipoJuridico; }
    public String getRubro() { return rubro; }
    public void setRubro(String rubro) { this.rubro = rubro; }
    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }
    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }
    public String getLocalidad() { return localidad; }
    public void setLocalidad(String localidad) { this.localidad = localidad; }
    public String getCiudad() { return ciudad; }
    public void setCiudad(String ciudad) { this.ciudad = ciudad; }
    public String getProvincia() { return provincia; }
    public void setProvincia(String provincia) { this.provincia = provincia; }
}
