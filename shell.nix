{ nixpkgs ? import
    (
      builtins.fetchTarball {
        name = "nixos-21.11";
        url = "https://github.com/NixOS/nixpkgs/archive/a7ecde854aee5c4c7cd6177f54a99d2c1ff28a31.tar.gz";
        sha256 = "162dywda2dvfj1248afxc45kcrg83appjd0nmdb541hl7rnncf02";
      }
    )
    {}
}:

with nixpkgs; let
  java = graalvm11-ce;
  sbt = (nixpkgs.sbt.override { jre = java; }).overrideAttrs (old: { doInstallCheck = false; });
  pkgs = [
    geckodriver
    java
    sbt
    gnupg
  ];

in
nixpkgs.stdenv.mkDerivation {
  name = "env";
  buildInputs = pkgs;
  shellHook = ''
    export SBT_OPTS="-Xmx2G"
  '';
}
