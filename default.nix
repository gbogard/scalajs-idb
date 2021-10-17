{ nixpkgs ? import
    (
      builtins.fetchTarball {
        name = "nixos-20.09";
        url = "https://github.com/NixOS/nixpkgs/archive/d0bb138fbc33b23dd19155fa218f61eed3cb685f.tar.gz";
        sha256 = "0dym3kg1wwl2npp3l3z7q8mk269kib0yphky2zb16ph42gbyly7l";
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
