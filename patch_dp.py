import sys
import io

path = r'c:\Users\Ihor\AndroidStudioProjects\TheTest1\app\src\main\assets\tab_viewer.html'
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()

old = """function dpFingering(data) {
    if(!data.length)return[];
    const MAX=22;
    function cands(d){
        const pr=d.notes.filter(n=>n.fret>0).map(n=>n.fret);
        if(!pr.length)return Array.from({length:MAX},(_,i)=>i+1);
        const mn=Math.max(1,Math.min(...pr)),lo=Math.max(1,mn-3),hi=Math.min(MAX,mn+2);
        return Array.from({length:hi-lo+1},(_,i)=>lo+i);
    }
    let dp=new Map();
    for(const pos of cands(data[0]))dp.set(pos,{cost:configCost(data[0].notes.map(n=>n.fret),pos),path:[pos]});
    for(let i=1;i<data.length;i++){
        const d=data[i],fr=d.notes.map(n=>n.fret),pd=data[i-1].duration,nd=new Map();
        for(const pos of cands(d)){
            const cc=configCost(fr,pos);let bc=Infinity,bp=null;
            for(const[pp,{cost:pc,path:pp2}]of dp){const t=pc+transCost(pp,pos,pd)+cc;if(t<bc){bc=t;bp=[...pp2,pos];}}
            nd.set(pos,{cost:bc,path:bp||[]});
        }
        dp=nd;
    }
    let best=null; for(const v of dp.values())if(!best||v.cost<best.cost)best=v;
    return best?best.path:[];
}"""

new_str = """function dpFingering(data) {
    if(!data.length)return[];
    const MAX=22;
    function cands(d){
        const pr=d.notes.filter(n=>n.fret>0).map(n=>n.fret);
        if(!pr.length)return Array.from({length:MAX},(_,i)=>i+1);
        const mn=Math.max(1,Math.min(...pr)),lo=Math.max(1,mn-3),hi=Math.min(MAX,mn+2);
        return Array.from({length:hi-lo+1},(_,i)=>lo+i);
    }
    let dp=new Map();
    for(const pos of cands(data[0]))dp.set(pos,{cost:configCost(data[0].notes.map(n=>n.fret),pos),prev:null,pos:pos});
    for(let i=1;i<data.length;i++){
        const d=data[i],fr=d.notes.map(n=>n.fret),pd=data[i-1].duration,nd=new Map();
        for(const pos of cands(d)){
            const cc=configCost(fr,pos);let bc=Infinity,bp=null;
            for(const[pp,{cost:pc}]of dp){const t=pc+transCost(pp,pos,pd)+cc;if(t<bc){bc=t;bp=dp.get(pp);}}
            nd.set(pos,{cost:bc,prev:bp,pos:pos});
        }
        dp=nd;
    }
    let best=null; for(const v of dp.values())if(!best||v.cost<best.cost)best=v;
    if(!best)return[];
    const path=[]; while(best){path.push(best.pos);best=best.prev;}
    return path.reverse();
}"""

if old.replace("\n", "\r\n") in content:
    content = content.replace(old.replace("\n", "\r\n"), new_str.replace("\n", "\r\n"))
elif old in content:
    content = content.replace(old, new_str)
else:
    print("Could not find pattern!")
    sys.exit(1)

with open(path, 'w', encoding='utf-8') as f:
    f.write(content)
print("Success")
