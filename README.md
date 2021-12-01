# TLinker
English | [简体中文](./README.zh.md)

TLinker is a BitTorrent tool currently with three functions:
- **AutoLink**: Find files described in a torrent file from source location, then create links in the destination location.
- **Link**: Copy directory structure from source location to destination location, and create links in the corresponding path of destination location.
- **Tree**: Create directory structure based on a torrent file.

## Applications

TLinker can help you if you want to:
- Change the file structure of a downloaded torrent, while keep this torrent seeding.
- Reseed a torrent whose file structure is different from the existing file structure.

## Usage
```shell
java -jar tlinker-*.*.jar <command> <options>
```

### AutoLink
```shell
java -jar tlinker-*.*.jar autolink -t <torrent> -s <source> -d <destination> [-h] [-e] [-q]
```
For each file described in the `torrent`, try to find it in the `source`, if found, create a link of this file in the corresponding relative path of `destination`.

- `-t`: (Required) Absolute or relative path to a torrent file, requires the read permission.
- `-s`: (Required) Absolute or relative path to the source directory, requires the read permission.
- `-d`: (Required) Absolute or relative path to the destination directory, requires the read and write permission.
- `-h`: (Optional) Use hard link if provided, otherwise use symbolic link.
- `-e`: (Optional) Generate a script of link commands instead of actually linking. The script file will be placed in the directory of the jar file, named `tlinker-####.bat` on Windows, or `tlinker-####.sh` on Linux.
- `-q`: (Optional) Quiet mode. Only output on errors.

### Link
```shell
java -jar tlinker-*.*.jar link -s <source> -d <destination> [-h]
```
Recursively create links of files inside `source` in the corresponding relative path of `destination`.

- `-s`: (Required) Absolute or relative path to the source directory, requires the read permission.
- `-d`: (Required) Absolute or relative path to the destination directory, requires the read and write permission.
- `-h`: (Optional) Use hard link if provided, otherwise use symbolic link.

### Tree
```shell
java -jar tlinker-*.*.jar tree -t <torrent> -d <destination> [-f]
```
Create the directory structure of `torrent` in `destination`.

- `-t`: (Required) Absolute or relative path to a torrent file, requires the read permission.
- `-d`: (Required) Absolute or relative path to the destination directory, requires the read and write permission.
- `-f`: (Optional) Also create empty files described in `torrent`.

### Examples
Assume that a torrent file `/downloads/Beethoven.torrent` describes these files:
> Piano sonatas/Pathétique.mp3\
> Piano sonatas/Mondschein.mp3\
> Symphonies/Eroica.mp3\
> Symphonies/Symphony No.5.mp3

Assume that a folder `/media/BeethovenCollections/` contains these files:
> 1790/Gassenhauer.mp3\
> 1790/Pathétique.mp3\
> 1800/Frühling.mp3\
> 1800/Mondschein.mp3\
> 1800/An die Hoffnung.mp3\
> 1800/Eroica.mp3\
> 1800/Symphony No.5.mp3

Assume that a folder `/home/Beethoven/` is empty.

#### AutoLink
```shell
java -jar tlinker-*.*.jar autolink -t /downloads/Beethoven.torrent -s /media/BeethovenCollections/ -d /home/Beethoven/
```

After running this command, `/home/Beethoven/` will look like this:
> **Piano sonatas/Pathétique.mp3** *-> /media/BeethovenCollections/1790/Pathétique.mp3*\
> **Piano sonatas/Mondschein.mp3** *-> /media/BeethovenCollections/1800/Mondschein.mp3*\
> **Symphonies/Eroica.mp3** *-> /media/BeethovenCollections/1800/Eroica.mp3*\
> **Symphonies/Symphony No.5.mp3** *-> /media/BeethovenCollections/1800/Symphony No.5.mp3*

#### Link
```shell
java -jar tlinker-*.*.jar link -s /media/BeethovenCollections/ -d /home/Beethoven/
```

After running this command, `/home/Beethoven/` will look like this:
> **1790/Gassenhauer.mp3** *-> /media/BeethovenCollections/1790/Gassenhauer.mp3*\
> **1790/Pathétique.mp3** *-> /media/BeethovenCollections/1790/Pathétique.mp3*\
> **1800/Frühling.mp3** *-> /media/BeethovenCollections/1800/Frühling.mp3*\
> **1800/Mondschein.mp3** *-> /media/BeethovenCollections/1800/Mondschein.mp3*\
> **1800/An die Hoffnung.mp3** *-> /media/BeethovenCollections/1800/An die Hoffnung.mp3*\
> **1800/Eroica.mp3** *-> /media/BeethovenCollections/1800/Eroica.mp3*\
> **1800/Symphony No.5.mp3** *-> /media/BeethovenCollections/1800/Symphony No.5.mp3*

#### Tree
```shell
java -jar tlinker-*.*.jar tree -t /downloads/Beethoven.torrent -d /home/Beethoven/
```

After running this command, `/home/Beethoven/` will look like this:
> **Piano sonatas/**\
> **Symphonies/**

## Compile
```shell
mvn clean package
```

## Technical Details

### Torrent file structure
`.torrent` file is the Bencode encoded text file, it stores the metadata of a torrent with key-value pairs, the following keys are required:
- `announce` *Tracker URL*
- `info`
    - `piece length` *Size of every piece*
    - `pieces` *SHA-1 checksum of each piece*
    - `name` *Name of this torrent*
    - `files` *File list*
        - `length` *Size of this file*
        - `path` *Relative path of this file*
    
[Wiki page](https://wiki.theory.org/BitTorrentSpecification#Metainfo_File_Structure)
    
The whole data of the torrent is separated into pieces in the order of file list, every piece has the same size, the SHA-1 checksum of each piece is stored in the torrent file. If a torrent contains multiple files, the data of a file may be placed in multiple pieces, and a piece may also contain the data from multiple files.

### Links
Many filesystems support linking. Creating a link of one file allows user to access this file from the link. There are hard link and symbolic link (also symlink or soft link), a hard link is a new reference to the original content, while a symbolic link is a special file containing information of the original file.

When the target of a symbolic link is moved or deleted, the symbolic link becomes invalid. When the target of a hard link is moved, the hard link remains valid. When the target of a hard link is deleted, the hard link remains valid, but it becomes invalid if the hard link is also deleted.

[Wikipedia page](https://en.wikipedia.org/wiki/Symbolic_link#Summary)

On Linux platforms, a symbolic link to a directory is the link to the directory itself, not its contents. Command `rm -rf link` will result in the deletion of this link, but command `rm -rf link/` will result in the recursive deletion of the original directories. For safety reasons, this program will not create links of directories, but links of its child files. This means every folder in the destination path is actually created.

### AutoLink
This program find files in the source location based on size and rank them based on path similarity, since BitTorrent does not provide per-file checksum, it is not possible to find files by checksum.

When running in autolink mode, this program gets a file list from the torrent. For each file in the file list, look for the `source` to find disk files with same size, and marked them as pre-match disk file of this file.

This program then reads the piece data. For each piece:
1. Find files included in this piece. If one (or more) of files do not have pre-match disk file, this piece will fail, and all files included in this piece will also fail. If one file have multiple pre-match disk file, this program will choose the most similar one.
2. Get the data of this piece. This piece may fully or partly include files, slice the partly included files and combine them to get the data of this piece.
3. Calculate checksum of this piece. If the calculated checksum is same with the checksum recorded in the torrent file, this piece is verified. Otherwise, this program will prompt to choose a disk file for those files with multiple pre-match disk files. If none of files have multiple pre-match disk files, this piece will fail. Failing in this step but not step 1 is probably because files with same size but different content exist in the source location.

A file may be included in multiple pieces, if all of these pieces are verified, this file is verified, otherwise this file is failed. Verified files will be linked and failed files will be printed.

## Issue
Feel free to submit issues and feature requests.

## License
This project is distributed under the Apache-2.0 License. See `LICENSE` for the full license text.