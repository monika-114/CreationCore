#!/usr/bin/env python3
"""Development-world integration check. Requires JAVA_HOME and GRADLE_BIN.
Uses temporary local RCON settings, restores server.properties, stops its server.
"""
import os, pathlib, secrets, signal, socket, struct, subprocess, time, urllib.parse
ROOT=pathlib.Path(__file__).resolve().parents[1]

class Rcon:
    def __init__(self,password):
        self.s=socket.create_connection(('127.0.0.1',25576),timeout=5)
        self.seq=0
        self.send(3,password)
    def readn(self,n):
        b=b''
        while len(b)<n:
            chunk=self.s.recv(n-len(b))
            if not chunk: raise RuntimeError('RCON closed')
            b+=chunk
        return b
    def send(self,kind,text):
        self.seq+=1; payload=struct.pack('<ii',self.seq,kind)+text.encode()+b'\0\0'
        self.s.sendall(struct.pack('<i',len(payload))+payload)
        while True:
            length=struct.unpack('<i',self.readn(4))[0]
            data=self.readn(length); seq,typ=struct.unpack('<ii',data[:8])
            if seq==-1: raise RuntimeError('RCON auth rejected')
            if seq==self.seq and (kind!=3 or typ==2): return data[8:-2].decode()
    def command(self,text): return self.send(2,text)

def main():
    props=ROOT/'run/server.properties'; original=props.read_bytes()
    password=secrets.token_hex(16)
    lines=[l for l in original.decode().splitlines() if not l.startswith(('enable-rcon=','rcon.port=','rcon.password=','broadcast-rcon-to-ops='))]
    props.write_text('\n'.join(lines)+f'\nenable-rcon=true\nrcon.port=25576\nrcon.password={password}\nbroadcast-rcon-to-ops=false\n')
    args=[os.environ['GRADLE_BIN'],'runServer','-x','createMinecraftArtifacts','-x','downloadAssets','--offline','--no-daemon','--no-configuration-cache','--console=plain','-Djavax.net.ssl.trustStore=/etc/ssl/certs/java/cacerts']
    p=urllib.parse.urlsplit(os.environ.get('HTTPS_PROXY',''))
    if p.hostname: args += ['-Dhttps.proxyHost='+p.hostname,'-Dhttps.proxyPort='+str(p.port or 80),'-Dhttp.proxyHost='+p.hostname,'-Dhttp.proxyPort='+str(p.port or 80)]
    proc=None; client=None
    log=ROOT/'build/crafter-smoke-server.log'
    try:
        with log.open('wb') as output:
            proc=subprocess.Popen(args,cwd=ROOT,stdout=output,stderr=subprocess.STDOUT,stdin=subprocess.DEVNULL,start_new_session=True)
            deadline=time.monotonic()+120
            while time.monotonic()<deadline:
                if proc.poll() is not None: raise RuntimeError('Server exited; inspect build/crafter-smoke-server.log')
                if 'RCON running' in log.read_text(errors='replace'):
                    try: client=Rcon(password); break
                    except (OSError,RuntimeError): pass
                time.sleep(1)
            if client is None: raise RuntimeError('Server startup timed out')
            print('Server ready; checking actual redstone crafting and container output.',flush=True)
            client.command('data modify storage creationcore:checks pass set value 1')
            for line in (ROOT/'tools/crafter-smoke-setup.mcfunction').read_text().splitlines():
                if line.strip(): client.command(line)
            time.sleep(1)
            def check(name,condition):
                response=client.command(condition+' run data get storage creationcore:checks pass')
                if not response.rstrip().endswith(': 1'): raise AssertionError(name+': '+response)
                print('PASS: '+name,flush=True)
            check('exclusive_recipe','execute if data block 100 200 99 Items[{id:"creationcore:cave_biome_spawn_egg",count:1}]')
            check('input_consumption','execute unless data block 100 200 100 Items[0]')
            check('vanilla_crafter_isolation','execute unless data block 105 200 99 Items[0] if data block 105 200 100 Items[{Slot:4b,id:"minecraft:egg"}]')
            check('ordinary_recipe_and_bucket','execute if data block 110 200 99 Items[{id:"creationcore:bottled_nothing",count:4}] if data block 110 200 99 Items[{id:"minecraft:bucket",count:1}]')
            check('disc_copy_and_original','execute if data block 115 200 99 Items[{Slot:0b,id:"minecraft:music_disc_13",count:1}] if data block 115 200 99 Items[{Slot:1b,id:"minecraft:music_disc_13",count:1}]')
            check('eight_barriers','execute if data block 120 200 99 Items[{id:"minecraft:barrier",count:8}]')
            check('blank_matter_ordinary_station','execute if data block 125 200 99 Items[{id:"creationcore:blank_matter",count:1}] if data block 125 200 99 Items[{id:"minecraft:glass_bottle",count:1}]')
            for cmd in ['setblock 136 200 100 air','setblock 135 200 99 minecraft:chest','setblock 135 200 100 creationcore:creative_crafter[orientation=north_up]', 'data merge block 135 200 100 {disabled_slots:[I;0,1,2,3,5,6,7,8]}','setblock 135 201 100 minecraft:hopper','data merge block 135 201 100 {Items:[{Slot:0b,id:"minecraft:oak_log",count:1}]}']:
                client.command(cmd)
            time.sleep(1)
            check('hopper_and_disabled_slots','execute if data block 135 200 100 Items[{Slot:4b,id:"minecraft:oak_log",count:1}]')
            client.command('setblock 136 200 100 minecraft:redstone_block'); time.sleep(0.5)
            check('hopper_fed_normal_crafting','execute if data block 135 200 99 Items[{id:"minecraft:oak_planks",count:4}]')
            client.command('save-all flush')
            print('PASS: world save completed with the new crafter blocks.',flush=True)
    finally:
        if client:
            try: client.command('stop')
            except OSError: pass
            client.s.close()
        if proc:
            try: proc.wait(timeout=20)
            except subprocess.TimeoutExpired:
                os.killpg(proc.pid,signal.SIGINT)
                try: proc.wait(timeout=10)
                except subprocess.TimeoutExpired: os.killpg(proc.pid,signal.SIGKILL); proc.wait()
        props.write_bytes(original)

if __name__=='__main__': main()
