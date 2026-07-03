package main

import (
    "encoding/hex"
    "flag"
    "fmt"
    "os"
    "strconv"
    "strings"
)

const version byte = 0x01

type command struct {
    name string
    code byte
}

var commands = map[string]command{
    "next":       {"Page next", 0x01},
    "prev":       {"Page previous", 0x02},
    "previous":   {"Page previous", 0x02},
    "select":     {"Select", 0x03},
    "back":       {"Back", 0x04},
    "menu":       {"Menu", 0x05},
    "sleep":      {"Sleep", 0x06},
    "wake":       {"Wake", 0x07},
    "scroll-up":  {"Scroll up", 0x08},
    "scroll-down":{"Scroll down", 0x09},
    "next-file":  {"Next chapter/file", 0x0A},
    "prev-file":  {"Previous chapter/file", 0x0B},
    "bookmark":   {"Toggle bookmark", 0x0C},
    "refresh":    {"Refresh / ghost cleanup", 0x0D},
}

func usage() {
    fmt.Fprintf(os.Stderr, "Usage:\n")
    fmt.Fprintf(os.Stderr, "  rrbp-cli encode <command> [--seq N] [--flags N] [--param N]\n")
    fmt.Fprintf(os.Stderr, "  rrbp-cli parse <hex bytes>\n")
    fmt.Fprintf(os.Stderr, "\nExamples:\n")
    fmt.Fprintf(os.Stderr, "  rrbp-cli encode next --seq 0\n")
    fmt.Fprintf(os.Stderr, "  rrbp-cli parse '01 00 01 00 00 00'\n")
}

func main() {
    if len(os.Args) < 2 {
        usage()
        os.Exit(2)
    }

    switch os.Args[1] {
    case "encode":
        encode(os.Args[2:])
    case "parse":
        parse(os.Args[2:])
    default:
        usage()
        os.Exit(2)
    }
}

func encode(args []string) {
    fs := flag.NewFlagSet("encode", flag.ExitOnError)
    seq := fs.Int("seq", 0, "sequence number 0-255")
    flags := fs.Int("flags", 0, "flags 0-255")
    param := fs.Int("param", 0, "parameter 0-255")
    _ = fs.Parse(args)

    if fs.NArg() < 1 {
        usage()
        os.Exit(2)
    }

    key := strings.ToLower(fs.Arg(0))
    cmd, ok := commands[key]
    if !ok {
        fmt.Fprintf(os.Stderr, "unknown command: %s\n", key)
        os.Exit(2)
    }
    for _, v := range []struct{ name string; value int }{{"seq", *seq}, {"flags", *flags}, {"param", *param}} {
        if v.value < 0 || v.value > 255 {
            fmt.Fprintf(os.Stderr, "%s out of range: %d\n", v.name, v.value)
            os.Exit(2)
        }
    }

    packet := []byte{version, byte(*seq), cmd.code, byte(*flags), byte(*param), 0x00}
    fmt.Printf("% X\n", packet)
}

func parse(args []string) {
    if len(args) < 1 {
        usage()
        os.Exit(2)
    }
    cleaned := strings.Join(args, "")
    cleaned = strings.ReplaceAll(cleaned, " ", "")
    cleaned = strings.ReplaceAll(cleaned, ":", "")
    cleaned = strings.ReplaceAll(cleaned, "-", "")

    packet, err := hex.DecodeString(cleaned)
    if err != nil {
        fmt.Fprintf(os.Stderr, "invalid hex: %v\n", err)
        os.Exit(2)
    }
    if len(packet) != 6 {
        fmt.Fprintf(os.Stderr, "invalid length: got %d want 6\n", len(packet))
        os.Exit(2)
    }
    if packet[0] != version {
        fmt.Fprintf(os.Stderr, "unsupported version: %d\n", packet[0])
        os.Exit(2)
    }

    name := fmt.Sprintf("unknown(0x%02X)", packet[2])
    for _, c := range commands {
        if c.code == packet[2] {
            name = c.name
            break
        }
    }

    fmt.Printf("version=%d seq=%d command=%s flags=0x%02X param=%d reserved=0x%02X\n",
        packet[0], packet[1], name, packet[3], packet[4], packet[5])
}

func parseByte(s string) byte {
    n, err := strconv.ParseUint(s, 0, 8)
    if err != nil {
        panic(err)
    }
    return byte(n)
}

var _ = parseByte
