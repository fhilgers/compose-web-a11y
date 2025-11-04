{
  inputs.nixpkgs.url = "github:NixOS/nixpkgs/nixpkgs-unstable";
  outputs =
    { nixpkgs, ... }@inputs:
    let
      system = "x86_64-linux";
      pkgs = import nixpkgs { inherit system; };
    in
    {
      devShells.${system}.default = pkgs.mkShell {
        LD_LIBRARY_PATH =
          with pkgs;
          pkgs.lib.makeLibraryPath [
            libpng
            xorg.libxkbfile
            libbsd
          ];
      };
    };
}
