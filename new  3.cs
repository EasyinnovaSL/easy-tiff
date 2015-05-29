using Newronia.Scheduling.Model.Dades;
using Newronia.Scheduling.Model.Fitxers;
using Newronia.Scheduling.Model.Resultats;
using NewroniaSolver.Finestres;
using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Text;
using System.Windows.Forms;

namespace NewroniaSolver.Dades
{
    class PlanificacioServeis
    {
        public Dictionary<DateTime, List<Servei>> dicDiesF;
        public List<PlanificacioDia> llista_planificacions;

        private List<Bucket> buckets;
        private List<Servei> llista_serveis, llista_serveis_pou;
        private Dictionary<string, Dictionary<DateTime, int>> matriu;
        private Dictionary<DateTime, Dictionary<string, int>> matriu_inversa;
        private Dictionary<string, HashSet<Bucket>> serveis_buckets;
        private Dictionary<string, HashSet<Bucket>> buckets_dies;
        private Dictionary<string, HashSet<DateTime>> visites_planificades_serveis;
        Random r = new Random(1);
        string nom_fitxer_log = "log.txt";

        public PlanificacioServeis()
        {
            dicDiesF = new Dictionary<DateTime, List<Servei>>();
            llista_planificacions = new List<PlanificacioDia>();
        }

        private void TuningBucketsDia()
        {
            int ncanvis = 0;
            int comprovats = 0;
            foreach (var b1 in buckets)
            {
                if (!b1.Dia.HasValue) continue;
                foreach (var b2 in buckets)
                {
                    if (!b2.Dia.HasValue) continue;
                    if (b1 == b2) continue;
                    if (b1.zona != b2.zona) continue;
                    if (b1.Dia.Value.Date != b2.Dia.Value.AddDays(7).Date && b1.Dia.Value.Date != b2.Dia.Value.AddDays(-7).Date) continue;
                    bool fer = true;
                    foreach (var b3 in buckets)
                    {
                        if (b3.Dia.Value.Date == b1.Dia.Value.Date)
                        {
                            foreach (var cp in b3.cpAssignats)
                                if (b2.ConteCP(cp.Id))
                                {
                                    fer = false;
                                    break;
                                }
                            foreach (var s1 in b3.serveis)
                            {
                                foreach (var s2 in b1.serveis)
                                    if (s1.Identificador == s2.Identificador)
                                    {
                                        fer = false;
                                        break;
                                    }
                                if (!fer) break;
                            }
                        }
                        if (!fer) break;
                        if (b3.Dia.Value.Date == b2.Dia.Value.Date)
                        {
                            foreach (var cp in b3.cpAssignats)
                                if (b1.ConteCP(cp.Id))
                                {
                                    fer = false;
                                    break;
                                }
                            foreach (var s1 in b3.serveis)
                            {
                                foreach (var s2 in b2.serveis)
                                    if (s1.Identificador == s2.Identificador)
                                    {
                                        fer = false;
                                        break;
                                    }
                                if (!fer) break;
                            }
                        }
                        if (!fer) break;
                    }
                    if (!fer) continue;

                    double puntuacio1 = PuntuacioBucketDia(b1.Dia.Value, b1) + PuntuacioBucketDia(b2.Dia.Value, b2);
                    double puntuacio2 = PuntuacioBucketDia(b2.Dia.Value, b1) + PuntuacioBucketDia(b1.Dia.Value, b2);
                    comprovats++;
                    if (puntuacio2 > puntuacio1)
                    {
                        var f = new StreamWriter(nom_fitxer_log, true); f.WriteLine("Canvis de dia al bucket. Zona " + b1.zona + ". Dies " + b1.Dia.Value.ToShortDateString() + " i " + b2.Dia.Value.ToShortDateString()); f.Close();
                        DateTime d = b1.Dia.Value;
                        b1.Dia = b2.Dia.Value;
                        b2.Dia = d;
                        ncanvis++;
                    }
                }
            }
            buckets_dies = new Dictionary<string, HashSet<Bucket>>();
            foreach (var b in buckets)
            {
                var sdia = b.Dia.Value.ToShortDateString();
                if (!buckets_dies.ContainsKey(sdia)) buckets_dies.Add(sdia, new HashSet<Bucket>());
                buckets_dies[sdia].Add(b);
            }
            var ff = new StreamWriter(nom_fitxer_log, true); ff.WriteLine(); ff.Close();
        }

        public void Planificar(bool tenir_en_compte_data_ultima_visita)
        {
            StreamWriter f = new StreamWriter(nom_fitxer_log, false); f.Close();

            // Crear llista de serveis a planificar (sense POU)
            CrearLlistesServeis();

            List<Bucket> buckets_trams = new List<Bucket>();
            var trams = TramsPlanificacio();
            for (var i = 0; i < trams.Count; i++)
            {
                f = new StreamWriter(nom_fitxer_log, true); f.WriteLine("Tram " + (i + 1) + ": " + trams[i].Item1.ToShortDateString() + " - " + trams[i].Item2.ToShortDateString()); f.Close();
            }
            var ultimes_visites_original = new Dictionary<string, DateTime>();
            foreach (var s in Context.Projecte.Serveis.Values)
                ultimes_visites_original.Add(s.Identificador, s.UltimaVisita);
            foreach (var tram in trams)
            {
                // Crear matriu completa, per dues setmanes
                CrearMatriu(tram.Item1, tram.Item1.AddDays(13));

                // Executar optimitzador zones
                FerBuckets(tram.Item3);

                // Precalcul Serveis Buckets
                serveis_buckets = ServeisBuckets(buckets);

                // Assignar buckets a dies
                buckets_dies = AssignarDiesBuckets(10);
                f = new StreamWriter(nom_fitxer_log, true); f.WriteLine("AA1"); FerLog(f); f.Close();
                TuningBucketsDia();

                // Repetir buckets per 3 mesos
                var data_fi = tram.Item1.AddMonths(3);
                if (ConfigClient.DataFi > data_fi) data_fi = ConfigClient.DataFi;
                RepetirBuckets(tenir_en_compte_data_ultima_visita, data_fi);

                EliminarBucketsFestius();

                serveis_buckets = ServeisBuckets(buckets);
                f = new StreamWriter(nom_fitxer_log, true); f.WriteLine("AA2"); FerLog(f); f.Close();

                CrearMatriu(tram.Item1, data_fi);

                // Planificar serveis
                var assignats = AssignarServeisBuckets(tenir_en_compte_data_ultima_visita, tram.Item1, data_fi);
                string s_assignats = StringAssignats(assignats);

                // Eliminar buckets fora de la planificacio
                EliminarBuckets(tram.Item1, tram.Item2);

                f = new StreamWriter(nom_fitxer_log, true); f.WriteLine("AA3"); FerLog(f); f.Close();
                f = new StreamWriter(nom_fitxer_log, true); f.WriteLine(s_assignats); f.Close();

                buckets_trams.AddRange(buckets);

                foreach (var b in buckets)
                    foreach (var s in b.serveis)
                        if (b.Dia.Value > Context.Projecte.Serveis[s.Identificador].UltimaVisita) Context.Projecte.Serveis[s.Identificador].UltimaVisita = b.Dia.Value;
            }

            buckets = buckets_trams;
            foreach (var s in Context.Projecte.Serveis.Values)
                s.UltimaVisita = ultimes_visites_original[s.Identificador];

            int n_b_cp = 0, n_b_no_cp = 0; foreach (var b in buckets) foreach (var s in b.serveis) if (b.ConteCP(s.CodiPostal)) n_b_cp++; else n_b_no_cp++;
            string str = FerString();
            f = new StreamWriter(nom_fitxer_log, true); f.WriteLine("Serveis en bucket del seu CP: " + n_b_cp); f.WriteLine("Serveis en bucket sense el seu CP: " + n_b_no_cp); f.WriteLine(); f.Close();

            visites_planificades_serveis = new Dictionary<string, HashSet<DateTime>>();
            foreach (var b in buckets) foreach (var s in b.serveis)
                {
                    if (!visites_planificades_serveis.ContainsKey(s.Identificador)) visites_planificades_serveis.Add(s.Identificador, new HashSet<DateTime>());
                    visites_planificades_serveis[s.Identificador].Add(b.Dia.Value);
                }

            f = new StreamWriter(nom_fitxer_log, true);
            Dictionary<string, List<string>> lbs = new Dictionary<string, List<string>>();
            foreach (var b in buckets)
                foreach (var s in b.serveis)
                {
                    if (!lbs.ContainsKey(b.Dia.Value.ToShortDateString())) lbs.Add(b.Dia.Value.ToShortDateString(), new List<string>());
                    lbs[b.Dia.Value.ToShortDateString()].Add(s.Identificador);
                }
            foreach (var kv in lbs)
            {
                f.Write("SB\t" + kv.Key + "\t");
                for (int i = 0; i < kv.Value.Count; i++)
                {
                    if (i > 0) f.Write(";");
                    f.Write(kv.Value[i]);
                }
                f.WriteLine();
            }
            f.WriteLine();
            f.Close();


            //Igualar càrrega buckets
            int ncanvis = 0;
            ncanvis = Tunning_ReduirKm_2(0, true);
            n_b_cp = 0; n_b_no_cp = 0; foreach (var b in buckets) foreach (var s in b.serveis) if (b.ConteCP(s.CodiPostal)) n_b_cp++; else n_b_no_cp++;
            str += Environment.NewLine + Environment.NewLine + FerString();
            f = new StreamWriter(nom_fitxer_log, true); f.WriteLine("Canvis tunning repartir km intradia: " + ncanvis); f.WriteLine("Serveis en bucket del seu CP: " + n_b_cp); f.WriteLine("Serveis en bucket sense el seu CP: " + n_b_no_cp); f.WriteLine(); f.Close();

            //Igualar càrrega buckets
            ncanvis = 0;
            ncanvis = Tunning_ReduirKm_2(4, false);
            n_b_cp = 0; n_b_no_cp = 0; foreach (var b in buckets) foreach (var s in b.serveis) if (b.ConteCP(s.CodiPostal)) n_b_cp++; else n_b_no_cp++;
            str += Environment.NewLine + Environment.NewLine + FerString();
            f = new StreamWriter(nom_fitxer_log, true); f.WriteLine("Canvis tunning repartir km: " + ncanvis); f.WriteLine("Serveis en bucket del seu CP: " + n_b_cp); f.WriteLine("Serveis en bucket sense el seu CP: " + n_b_no_cp); f.WriteLine(); f.Close();

            //ncanvis = RepartirCarregaBucketsGermans();
            //n_b_cp = 0; n_b_no_cp = 0; foreach (var b in buckets) foreach (var s in b.serveis) if (b.ConteCP(s.CodiPostal)) n_b_cp++; else n_b_no_cp++;
            //str += Environment.NewLine + Environment.NewLine + FerString();
            //f = new StreamWriter(nom_fitxer_log, true); f.WriteLine("Canvis repartir buckets germans: " + ncanvis); f.WriteLine("Serveis en bucket del seu CP: " + n_b_cp); f.WriteLine("Serveis en bucket sense el seu CP: " + n_b_no_cp); f.WriteLine(); f.Close();

            //var zones_no = new HashSet<string>(); zones_no.Add("ZONA4");
            ncanvis = RepartirCarregaBucketsZona();
            n_b_cp = 0; n_b_no_cp = 0; foreach (var b in buckets) foreach (var s in b.serveis) if (b.ConteCP(s.CodiPostal)) n_b_cp++; else n_b_no_cp++;
            str += Environment.NewLine + Environment.NewLine + FerString();
            f = new StreamWriter(nom_fitxer_log, true); f.WriteLine("Canvis repartir buckets zona: " + ncanvis); f.WriteLine("Serveis en bucket del seu CP: " + n_b_cp); f.WriteLine("Serveis en bucket sense el seu CP: " + n_b_no_cp); f.WriteLine(); f.Close();

            ncanvis = Tunning_Igualar(0, true);
            n_b_cp = 0; n_b_no_cp = 0; foreach (var b in buckets) foreach (var s in b.serveis) if (b.ConteCP(s.CodiPostal)) n_b_cp++; else n_b_no_cp++;
            str += Environment.NewLine + Environment.NewLine + FerString();
            f = new StreamWriter(nom_fitxer_log, true); f.WriteLine("Canvis tunning igualar intradia: " + ncanvis); f.WriteLine("Serveis en bucket del seu CP: " + n_b_cp); f.WriteLine("Serveis en bucket sense el seu CP: " + n_b_no_cp); f.WriteLine(); f.Close();

            ncanvis = Tunning_Igualar(4, false);
            n_b_cp = 0; n_b_no_cp = 0; foreach (var b in buckets) foreach (var s in b.serveis) if (b.ConteCP(s.CodiPostal)) n_b_cp++; else n_b_no_cp++;
            str += Environment.NewLine + Environment.NewLine + FerString();
            f = new StreamWriter(nom_fitxer_log, true); f.WriteLine("Canvis tunning igualar: " + ncanvis); f.WriteLine("Serveis en bucket del seu CP: " + n_b_cp); f.WriteLine("Serveis en bucket sense el seu CP: " + n_b_no_cp); f.WriteLine(); f.Close();

            //ncanvis = RemoureServeisAllunyats();
            //n_b_cp = 0; n_b_no_cp = 0; foreach (var b in buckets) foreach (var s in b.serveis) if (b.ConteCP(s.CodiPostal)) n_b_cp++; else n_b_no_cp++;
            //str += Environment.NewLine + Environment.NewLine + FerString();
            //f = new StreamWriter(nom_fitxer_log, true); f.WriteLine("Canvis moure allunyats: " + ncanvis); f.WriteLine("Serveis en bucket del seu CP: " + n_b_cp); f.WriteLine("Serveis en bucket sense el seu CP: " + n_b_no_cp); f.Close();

            //new Missatge(str, "").ShowDialog();
            f = new StreamWriter(nom_fitxer_log, true); f.WriteLine(); f.WriteLine(str); f.Close();

            //Mensuals();
            //EliminarVacances();
            MinimitzarFestius();
            //EquilibrarMensuals();
            if (tenir_en_compte_data_ultima_visita) ServeisEndarrerits(true);
            //ControlServeisRepetitsDia();

            n_b_cp = 0; n_b_no_cp = 0; foreach (var b in buckets) foreach (var s in b.serveis) if (b.ConteCP(s.CodiPostal)) n_b_cp++; else n_b_no_cp++;
            f = new StreamWriter(nom_fitxer_log, true); f.WriteLine(); f.WriteLine(FerString()); f.Close();
            f = new StreamWriter(nom_fitxer_log, true); f.WriteLine("Serveis en bucket del seu CP: " + n_b_cp); f.WriteLine("Serveis en bucket sense el seu CP: " + n_b_no_cp); f.WriteLine(); f.Close();

            // Crear Planificacions
            PlanificacionsPOU();

            Planificacions();
        }

        private void EquilibrarMensuals()
        {
            foreach (var b in buckets)
            {
                if (b.bucketB == null) continue;
                for (int i_s=0;i_s<b.serveis.Count;i_s++)
                {
                    var s = b.serveis[i_s];
                    if (s.Frequencia.TotalDays != 20) continue;
                    var l = new List<Bucket>();
                    foreach (var bbb in buckets)
                    {
                        foreach (var ss in bbb.serveis)
                            if (ss.Identificador == s.Identificador)
                            {
                                l.Add(bbb);
                                break;
                            }
                    }

                    var lds = new List<DateTime>();
                    foreach (var bs in l) lds.Add(bs.Dia.Value);
                    lds.Sort();
                    bool ok = true;
                    for (int i = 1; i < lds.Count; i++)
                        if ((lds[i] - lds[i - 1]).TotalDays != 28)
                        {
                            ok = false;
                            break;
                        }
                    if (!ok) continue;

                    var ldsb = new List<DateTime>();
                    foreach (var bs in l) if (bs.bucketB != null) ldsb.Add(bs.bucketB.Dia.Value);
                    ldsb.Sort();
                    ok = true;
                    for (int i = 1; i < ldsb.Count; i++)
                        if ((ldsb[i] - ldsb[i - 1]).TotalDays != 28)
                        {
                            ok = false;
                            break;
                        }
                    if (!ok) continue;

                    if (lds.Count != ldsb.Count) continue;

                    int dif_ara = 0;
                    int dif_canvi = 0;
                    foreach (var bs in l)
                    {
                        dif_ara += Math.Abs(bs.serveis.Count - bs.bucketB.serveis.Count);
                        dif_canvi += Math.Abs(bs.serveis.Count - 1 - (bs.bucketB.serveis.Count + 1));
                    }
                    if (dif_canvi < dif_ara * .8)
                    {
                        var sms = "";
                        foreach (var bb in l)
                        {
                            sms += bb.Dia.Value.ToShortDateString() + "->" + bb.bucketB.Dia.Value.ToShortDateString() + " ";
                            bb.bucketB.serveis.Add(s);
                            bb.serveis.Remove(s);
                        }
                        var f = new StreamWriter(nom_fitxer_log, true); f.WriteLine("Mensual mogut: " + s.Nom + " " + sms); f.Close();
                        i_s--;
                    }
                }
            }
            var ff = new StreamWriter(nom_fitxer_log, true); ff.WriteLine(); ff.Close();
        }

        private void Mensuals()
        {
            var fets = new HashSet<Bucket>();
            foreach (var b in buckets)
            {
                if (fets.Contains(b)) continue;
                var cps = new Dictionary<string, List<Servei>>();
                foreach (var s in b.serveis)
                {
                    if (!cps.ContainsKey(s.CodiPostal)) cps.Add(s.CodiPostal, new List<Servei>());
                    cps[s.CodiPostal].Add(s);
                }
                var cps_mensuals = new Dictionary<string, List<Servei>>();
                foreach (var cp in cps.Keys)
                {
                    bool tots_mensuals = true;
                    foreach (var s in cps[cp])
                        if (s.Frequencia.TotalDays < 20)
                        {
                            tots_mensuals = false;
                            break;
                        }
                    if (tots_mensuals) cps_mensuals.Add(cp, cps[cp]);
                }
                foreach (var cp in cps_mensuals.Keys)
                {
                    foreach (var b2 in buckets)
                    {
                        if (fets.Contains(b2)) continue;
                        if (Math.Abs((b2.Dia.Value - b.Dia.Value).TotalDays) != 14) continue;
                        var serveis_cp = new List<Servei>();
                        foreach (var s in b2.serveis) if (s.CodiPostal == cp) serveis_cp.Add(s);
                        if (serveis_cp.Count == 0) continue;

                        int n_bucket1 = b.serveis.Count;
                        int n_bucket2 = b2.serveis.Count;
                        int ncp_bucket1 = cps_mensuals[cp].Count;
                        int ncp_bucket2 = serveis_cp.Count;

                        int n_bucket1_2a1 = n_bucket1 + ncp_bucket2;
                        int n_bucket2_2a1 = n_bucket2 - ncp_bucket2;
                        int n_bucket1_1a2 = n_bucket1 - ncp_bucket1;
                        int n_bucket2_1a2 = n_bucket2 + ncp_bucket1;
                        int dif_2a1 = Math.Abs(n_bucket1_2a1 - n_bucket2_2a1);
                        int dif_1a2 = Math.Abs(n_bucket1_1a2 - n_bucket2_1a2);
                        if (dif_1a2 < dif_2a1)
                        {
                            bool hi_es = false;
                            foreach (var s in cps_mensuals[cp])
                            {
                                foreach (var bb in buckets)
                                {
                                    if (bb == b2) continue;
                                    if (bb.Dia.Value != b2.Dia.Value) continue;
                                    foreach (var ss in bb.serveis)
                                        if (ss.Identificador == s.Identificador)
                                        {
                                            hi_es = true;
                                            break;
                                        }
                                    if (hi_es) break;
                                }
                                if (hi_es) break;
                            }
                            if (!hi_es)
                            {
                                foreach (var s in cps_mensuals[cp])
                                {
                                    b2.serveis.Add(s);
                                    b.serveis.Remove(s);
                                }
                            }
                        }
                        else
                        {
                            bool hi_es = false;
                            foreach (var s in serveis_cp)
                            {
                                foreach (var bb in buckets)
                                {
                                    if (bb == b) continue;
                                    if (bb.Dia.Value != b.Dia.Value) continue;
                                    foreach (var ss in bb.serveis)
                                        if (ss.Identificador == s.Identificador)
                                        {
                                            hi_es = true;
                                            break;
                                        }
                                    if (hi_es) break;
                                }
                                if (hi_es) break;
                            }
                            if (!hi_es)
                            {
                                foreach (var s in serveis_cp)
                                {
                                    b.serveis.Add(s);
                                    b2.serveis.Remove(s);
                                }
                            }
                        }

                        fets.Add(b);
                        fets.Add(b2);
                    }
                }
            }
        }

        private void EliminarVacances()
        {
            foreach (var b in buckets)
            {
                for (int i = 0; i < b.serveis.Count; i++)
                {
                    var client = b.serveis[i];
                    var df = b.Dia.Value;
                    if (df >= client.IniciVacancesAltres && df <= client.FiVacancesAltres)
                        b.serveis.RemoveAt(i--);
                }
            }
        }

        private void MinimitzarFestius()
        {
            var fj1 = new StreamWriter(nom_fitxer_log, true); fj1.WriteLine(); fj1.Close();
            foreach (var b in buckets)
            {
                // Crear llista de serveis d'aquest bucket que tenen festiu aquest dia
                List<Servei> lfestius = new List<Servei>();
                foreach (var s in b.serveis)
                    if (Context.FestiuDia(s, b.Dia.Value))
                        lfestius.Add(s);
                if (lfestius.Count == 0) continue;

                if (lfestius.Count * 100 / b.serveis.Count <= 25)
                {
                    // Moure només els serveis festius a un altre dia
                    foreach (var s1 in lfestius)
                    {
                        Bucket millor_dia = null;
                        double min_dist = -1;
                        DateTime dia = DateTime.Now;
                        var m = s1.Marge.TotalDays;
                        if (m == 0) m = 1;
                        var d1 = b.Dia.Value.AddDays(-m);
                        var d2 = b.Dia.Value.AddDays(m);
                        foreach (var b2 in buckets)
                        {
                            if (b.Dia.Value == b2.Dia.Value) continue;
                            if (b2.Dia.Value.Date < d1.Date || b2.Dia.Value.Date > d2.Date) continue;
                            int indexdia = -1;
                            switch (b2.Dia.Value.DayOfWeek)
                            {
                                case DayOfWeek.Monday: indexdia = 0; break;
                                case DayOfWeek.Tuesday: indexdia = 1; break;
                                case DayOfWeek.Wednesday: indexdia = 2; break;
                                case DayOfWeek.Thursday: indexdia = 3; break;
                                case DayOfWeek.Friday: indexdia = 4; break;
                                case DayOfWeek.Saturday: indexdia = 5; break;
                            }
                            if (indexdia < s1.Preferencies.Length && (s1.Preferencies[indexdia] == 'T'))
                                continue;
                            if (Context.FestiuDia(s1, b2.Dia.Value)) continue;

                            bool trobat = false;
                            foreach (var s2 in b2.serveis)
                                if (s2.Identificador == s1.Identificador)
                                {
                                    trobat = true;
                                    break;
                                }
                            if (!trobat)
                            {
                                foreach (var b3 in buckets)
                                {
                                    if (b3 != b2 && b3.Dia.Value == b2.Dia.Value)
                                        foreach (var s2 in b3.serveis)
                                            if (s2.Identificador == s1.Identificador)
                                            {
                                                trobat = true;
                                                break;
                                            }
                                    if (trobat) break;
                                }
                            }

                            if (!trobat)
                            {
                                // Solució candidata de ser afegit
                                double dist = b2.DistanciaServei(s1) + (b2.Dia.Value - b.Dia.Value).TotalDays;
                                if (millor_dia == null || dist < min_dist)
                                {
                                    millor_dia = b2;
                                    min_dist = dist;
                                    dia = b2.Dia.Value;
                                }
                            }
                        }
                        if (millor_dia != null)
                        {
                            b.serveis.Remove(s1);
                            millor_dia.serveis.Add(s1);
                            var f = new StreamWriter(nom_fitxer_log, true); f.WriteLine("Festiu mogut: " + s1.Nom + ". " + b.Dia.Value.ToShortDateString() + " -> " + millor_dia.Dia.Value.ToShortDateString()); f.Close();
                        }
                        else
                        {
                            var f = new StreamWriter(nom_fitxer_log, true); f.WriteLine("Festiu impossible de moure: " + s1.Nom + ". " + b.Dia.Value.ToShortDateString()); f.Close();
                        }
                    }
                }
                else
                {
                    // Intercanviar aquest bucket sencer amb el d'un altre dia
                    double min_dist = 0;
                    Bucket millor_bucket = null;
                    foreach (var b2 in buckets)
                    {
                        if (Math.Abs((b.Dia.Value.Date - b2.Dia.Value.Date).TotalDays) != 1) continue;
                        if (!BucketDiaValid(b, b2.Dia.Value, b2)) continue;
                        if (!BucketDiaValid(b2, b.Dia.Value, b)) continue;

                        var dist = b2.DistanciaBucket(b);
                        if (b2.zona == b.zona) dist = 0;
                        if (millor_bucket == null || dist < min_dist)
                        {
                            min_dist = dist;
                            millor_bucket = b2;
                        }
                    }
                    if (millor_bucket != null)
                    {
                        var f = new StreamWriter(nom_fitxer_log, true); f.WriteLine("Bucket festiu mogut: " + b.zona + " " + b.Dia.Value.ToShortDateString() + " -> " + millor_bucket.zona + " " + millor_bucket.Dia.Value.ToShortDateString() + ". Dist: " + min_dist); f.Close();
                        var aux = b.Dia.Value;
                        b.Dia = millor_bucket.Dia;
                        millor_bucket.Dia = aux;
                    }
                    else
                    {
                        var f = new StreamWriter(nom_fitxer_log, true); f.WriteLine("Bucket festiu impossible de moure: " + b.zona + " " + b.Dia.Value.ToShortDateString()); f.Close();
                    }
                }
            }
            var fj = new StreamWriter(nom_fitxer_log, true); fj.WriteLine(); fj.Close();
        }

        private bool BucketDiaValid(Bucket b, DateTime dia, Bucket bucket_b)
        {
            foreach (var s in b.serveis)
            {
                if (Context.FestiuDia(s, dia)) return false;
                int indexdia = -1;
                switch (dia.DayOfWeek)
                {
                    case DayOfWeek.Monday: indexdia = 0; break;
                    case DayOfWeek.Tuesday: indexdia = 1; break;
                    case DayOfWeek.Wednesday: indexdia = 2; break;
                    case DayOfWeek.Thursday: indexdia = 3; break;
                    case DayOfWeek.Friday: indexdia = 4; break;
                    case DayOfWeek.Saturday: indexdia = 5; break;
                }
                if (indexdia < s.Preferencies.Length && (s.Preferencies[indexdia] == 'T')) return false;

                foreach (var b3 in buckets)
                {
                    if (b3 != bucket_b)
                        if (b3.Dia.Value == dia)
                            foreach (var s2 in b3.serveis)
                                if (s2.Identificador == s.Identificador)
                                    return false;
                }
            }
            return true;
        }

        private List<Tuple<DateTime, DateTime, HashSet<string>>> TramsPlanificacio()
        {
            var trams = new List<Tuple<DateTime, DateTime, HashSet<string>>>();
            for (DateTime d = ConfigClient.DataInici; d <= ConfigClient.DataFi; d = d.AddDays(1))
            {
                if (d.DayOfWeek == DayOfWeek.Saturday || d.DayOfWeek == DayOfWeek.Sunday) continue;
                HashSet<string> hvehicles = new HashSet<string>();
                foreach (var v in Context.Projecte.Vehicles)
                {
                    if (v.Value.Competencies.Contains("ReqPOU")) continue;
                    if (d >= v.Value.IniciVacances && d <= v.Value.FiVacances) continue;
                    if (d >= v.Value.IniciVacances && d <= v.Value.FiVacances) continue;
                    hvehicles.Add(v.Key);
                }
                if (trams.Count == 0) trams.Add(new Tuple<DateTime, DateTime, HashSet<string>>(d, d, hvehicles));
                else
                {
                    bool igual = true;
                    if (trams.Last().Item3.Count != hvehicles.Count) igual = false;
                    if (igual) foreach (var idv in trams.Last().Item3) if (!hvehicles.Contains(idv)) { igual = false; break; }
                    if (igual || (d - trams.Last().Item1).TotalDays < 14) trams[trams.Count-1] = new Tuple<DateTime, DateTime, HashSet<string>>(trams.Last().Item1, d, trams.Last().Item3);
                    else trams.Add(new Tuple<DateTime, DateTime, HashSet<string>>(d, d, hvehicles));
                }
            }
            return trams;
        }

        private void FerBuckets(HashSet<string> vehicles)
        {
            TimeSpan ts;
            var opt = Context.OptimitzarZones(llista_serveis, vehicles.Count, out ts);
            //new Missatge(opt.RetornaInforme().Replace("\n", Environment.NewLine), "").ShowDialog();
            if (opt.Calculat)
            {
                StreamWriter f = new StreamWriter(nom_fitxer_log, true);
                f.WriteLine(opt.RetornaInforme().Replace("\n", Environment.NewLine));
                f.WriteLine("Temps: " + ts.TotalSeconds.ToString());
                f.WriteLine();
                f.Close();
            }
            buckets = opt.Buckets();
        }

        private void ServeisEndarrerits(bool nomes_setmanals = false)
        {
            string real = ConfigClient.Directori + "\\Schedules\\" + ConfigClient.Planificacio;
            Dictionary<string, HashSet<string>> ultimes_visites_real = new Dictionary<string, HashSet<string>>();

            // Calcular últimes visites
            if (Directory.Exists(real))
            {
                foreach (string fitxer in Directory.GetFiles(real))
                {
                    string sss = Path.GetFileNameWithoutExtension(fitxer);
                    string[] data = sss.Split('.');
                    if (data.Length < 3) continue;

                    Solucio sol = SolucioXml.CarregarSenseProjecte(fitxer);
                    foreach (var ruta in sol.Rutes.Values)
                        foreach (var ss in ruta.Serveis)
                        {
                            if (!ultimes_visites_real.ContainsKey(ss.Servei.Identificador)) ultimes_visites_real.Add(ss.Servei.Identificador, new HashSet<string>());
                            ultimes_visites_real[ss.Servei.Identificador].Add(Path.GetFileName(fitxer));
                        }
                }
                foreach (var b in buckets)
                {
                    foreach (var ss in b.serveis)
                    {
                        if (!ultimes_visites_real.ContainsKey(ss.Identificador)) ultimes_visites_real.Add(ss.Identificador, new HashSet<string>());
                        ultimes_visites_real[ss.Identificador].Add(b.Dia.Value.Year + "." + b.Dia.Value.Month + "." + b.Dia.Value.Day);
                    }
                }
            }

            // Fer llista de serveis endarrerits
            var l = new List<Tuple<Servei, DateTime, DateTime>>();
            foreach (var s1 in Context.Projecte.Serveis.Values)
            {
                if (!s1.Planificar) continue;
                int maxim_dies = 1; // màxim de dies de diferència que hi poden haver entre la visita planificada i la que tocaria per freqüència
                int marge_dies = 1; // marge de dies en què s'afegirà una visita adicional que falti, al voltant del dia de la visita que toca (per ferqüència)
                int minim_dies = 1; // mínim de dies que hi ha d'haver entre una visita adicional afegida i la següent visita planificada
                if (s1.Frequencia.TotalDays == 5)
                {
                    maxim_dies = 1; marge_dies = 1; minim_dies = 2;
                }
                else if (s1.Frequencia.TotalDays == 10)
                {
                    if (nomes_setmanals) continue;
                    maxim_dies = 4; marge_dies = 2; minim_dies = 5;
                }
                else if (s1.Frequencia.TotalDays == 20)
                {
                    if (nomes_setmanals) continue;
                    maxim_dies = 8; marge_dies = 3; minim_dies = 10;
                }
                else continue;

                List<DateTime> visites = new List<DateTime>();
                if (ultimes_visites_real.ContainsKey(s1.Identificador))
                    foreach (var vis in ultimes_visites_real[s1.Identificador])
                        visites.Add(new DateTime(int.Parse(vis.Split('.')[0]), int.Parse(vis.Split('.')[1]), int.Parse(vis.Split('.')[2])));
                visites.Sort();

                for (int i = 0; i < visites.Count; i++)
                {
                    if (visites[i] < ConfigClient.DataInici) continue;
                    DateTime visita_anterior = s1.UltimaVisita;
                    if (i - 1 >= 0 && visites[i - 1] > s1.UltimaVisita) visita_anterior = visites[i - 1];
                    var visita_prevista = Context.PassaDies(visita_anterior, (int)s1.Frequencia.TotalDays);
                    while ((visites[i] - visita_prevista).TotalDays > maxim_dies)
                    {
                        var d1 = Context.PassaDies(visita_prevista,-marge_dies);
                        var d2 = Context.PassaDies(visita_prevista, marge_dies);

                        if (d1 < ConfigClient.DataInici) d1 = ConfigClient.DataInici;
                        if (d2 < ConfigClient.DataInici) d2 = ConfigClient.DataInici;

                        if ((visites[i] - d2).TotalDays <= minim_dies) d2 = visites[i].AddDays(-minim_dies);

                        visita_prevista = d1.AddDays((d2 - d1).TotalDays / 2);
                        if (d2 < d1)
                        {
                            var f = new StreamWriter("log.txt", true); f.WriteLine("Endarrerit F" + s1.Frequencia.TotalDays + " sense possibilitat de resolució! '" + s1.Nom + "' visita anterior: " + visita_anterior.ToShortDateString() + ". Visita planificada " + visites[i]); f.Close();
                        }
                        else
                        {
                            var tup = new Tuple<Servei, DateTime, DateTime>(s1, d1, d2);
                            l.Add(tup);
                            var md = BuscaMillorDia(tup);
                            if (md != null) visita_prevista = md.Dia.Value;
                            var f = new StreamWriter("log.txt", true); f.WriteLine("Afegit endarrerit F" + s1.Frequencia.TotalDays + " adicional '" + s1.Nom + "' entre " + d1.ToShortDateString() + " i " + d2.ToShortDateString()); f.Close();
                        }
                        visita_prevista = Context.PassaDies(visita_prevista, (int)s1.Frequencia.TotalDays);
                    }
                }
            }
            
            // Bisetmanals
            foreach (var s1 in Context.Projecte.Serveis.Values)
            {
                if (!s1.Planificar) continue;
                if (s1.Frequencia.TotalDays != 2) continue;

                int setmana_inicial_planif = Context.Setmana(ConfigClient.DataInici);
                int setmana_final_planif = Context.Setmana(ConfigClient.DataFi);
                for (int setmana = setmana_inicial_planif; setmana <= setmana_final_planif; setmana++)
                {
                    int fet = 0;
                    DateTime dia_fet = DateTime.Today;
                    foreach (var vis in ultimes_visites_real[s1.Identificador])
                    {
                        DateTime dia = new DateTime(int.Parse(vis.Split('.')[0]), int.Parse(vis.Split('.')[1]), int.Parse(vis.Split('.')[2]));
                        int set = Context.Setmana(dia);
                        if (setmana == set)
                        {
                            fet++;
                            dia_fet = dia;
                        }
                    }
                    if (fet == 0)
                    {
                        DateTime d1, d2;
                        d1 = ConfigClient.DataInici.AddDays(7 * (setmana - setmana_inicial_planif));
                        while (d1.DayOfWeek != DayOfWeek.Monday) d1 = d1.AddDays(-1);
                        d2 = d1.AddDays(2);
                        l.Add(new Tuple<Servei, DateTime, DateTime>(s1, d1, d2));
                        var f = new StreamWriter("log.txt", true); f.WriteLine("Afegit bisetmanal adicional '" + s1.Nom + "' entre " + d1.ToShortDateString() + " i " + d2.ToShortDateString()); f.Close();
                        d1 = d2.AddDays(1);
                        d2 = d1.AddDays(3);
                        l.Add(new Tuple<Servei, DateTime, DateTime>(s1, d1, d2));
                        f = new StreamWriter("log.txt", true); f.WriteLine("Afegit bisetmanal adicional '" + s1.Nom + "' entre " + d1.ToShortDateString() + " i " + d2.ToShortDateString()); f.Close();
                    }
                    else if (fet == 1)
                    {
                        DateTime d1, d2;
                        d1 = ConfigClient.DataInici.AddDays(7 * (setmana - setmana_inicial_planif));
                        while (d1.DayOfWeek != DayOfWeek.Monday) d1 = d1.AddDays(-1);
                        d2 = d1.AddDays(6);
                        if (dia_fet <= d1.AddDays(2))
                            d1 = d1.AddDays(3);
                        else
                            d2 = d1.AddDays(2);
                        l.Add(new Tuple<Servei, DateTime, DateTime>(s1, d1, d2));
                        var f = new StreamWriter("log.txt", true); f.WriteLine("Afegit bisetmanal adicional '" + s1.Nom + "' entre " + d1.ToShortDateString() + " i " + d2.ToShortDateString()); f.Close();
                    }
                }
            }
            int nfets = 1;
            int total = l.Count;
            foreach (var tup in l)
            {
                Application.DoEvents();
                nfets++;
                // No s'ha planificat i s'havia d'haver fet --> l'afegim hard
                Bucket millor_dia = BuscaMillorDia(tup);
                var s1 = tup.Item1;
                
                string sl = "Servei endarrerit: " + tup.Item1.Nom;
                if (millor_dia != null)
                {
                    sl += " --> " + millor_dia.Dia.Value.ToShortDateString();
                    s1.CanviarData(millor_dia.Dia.Value);
                    millor_dia.serveis.Add(s1);
                }
                var f = new StreamWriter("log.txt", true); f.WriteLine(sl); f.Close();
            }
        }

        Bucket BuscaMillorDia(Tuple<Servei, DateTime, DateTime> tup)
        {
            Bucket millor_dia = null;
            double min_dist = -1;
            DateTime dia = DateTime.Now;
            var d1 = tup.Item2;
            var d2 = tup.Item3;
            var s1 = tup.Item1;
            foreach (var b in buckets)
            {
                int indexdia = -1;
                switch (b.Dia.Value.DayOfWeek)
                {
                    case DayOfWeek.Monday: indexdia = 0; break;
                    case DayOfWeek.Tuesday: indexdia = 1; break;
                    case DayOfWeek.Wednesday: indexdia = 2; break;
                    case DayOfWeek.Thursday: indexdia = 3; break;
                    case DayOfWeek.Friday: indexdia = 4; break;
                    case DayOfWeek.Saturday: indexdia = 5; break;
                }
                if (indexdia < s1.Preferencies.Length && (s1.Preferencies[indexdia] == 'T' || s1.Preferencies[indexdia] == 'X'))
                    continue;

                if (b.Dia.Value.Date >= d1.Date && b.Dia.Value.Date <= d2.Date)
                {
                    bool trobat = false;
                    foreach (var s in b.serveis)
                        if (s.Identificador == tup.Item1.Identificador)
                        {
                            trobat = true;
                            break;
                        }
                    if (!trobat)
                    {
                        foreach (var b2 in buckets)
                        {
                            if (b2 != b && b2.Dia.Value == b.Dia.Value)
                                foreach (var s in b2.serveis)
                                    if (s.Identificador == tup.Item1.Identificador)
                                    {
                                        trobat = true;
                                        break;
                                    }
                            if (trobat) break;
                        }
                    }

                    if (!trobat)
                    {
                        // Solució candidata de ser afegit
                        double dist = b.DistanciaServei(s1);
                        if (millor_dia == null || dist < min_dist)
                        {
                            millor_dia = b;
                            min_dist = dist;
                            dia = b.Dia.Value;
                        }
                    }
                }
            }
            if (millor_dia == null)
            {
                foreach (var b in buckets)
                {
                    int indexdia = -1;
                    switch (b.Dia.Value.DayOfWeek)
                    {
                        case DayOfWeek.Monday: indexdia = 0; break;
                        case DayOfWeek.Tuesday: indexdia = 1; break;
                        case DayOfWeek.Wednesday: indexdia = 2; break;
                        case DayOfWeek.Thursday: indexdia = 3; break;
                        case DayOfWeek.Friday: indexdia = 4; break;
                        case DayOfWeek.Saturday: indexdia = 5; break;
                    }
                    if (indexdia < s1.Preferencies.Length && (s1.Preferencies[indexdia] == 'T'))
                        continue;

                    if (b.Dia.Value.Date >= d1.Date && b.Dia.Value.Date <= d2.Date)
                    {
                        // Solució candidata de ser afegit
                        double dist = b.DistanciaServei(s1);
                        if (millor_dia == null || dist < min_dist)
                        {
                            millor_dia = b;
                            min_dist = dist;
                            dia = b.Dia.Value;
                        }
                    }
                }
            }
            return millor_dia;
        }

        private void EliminarBucketsFestius()
        {
            string fitxer_festius = ConfigClient.Directori + "\\LocalCommunication\\festius.txt";
            if (!File.Exists(fitxer_festius)) return;
            StreamReader f = new StreamReader(fitxer_festius);
            while (!f.EndOfStream)
            {
                string sdia = f.ReadLine();
                if (sdia.Length == 0) continue;
                DateTime dia;
                if (DateTime.TryParse(sdia, out dia))
                {
                    for (int i = 0; i < buckets.Count; i++)
                    {
                        if (buckets[i].Dia.Value.Date == dia.Date)
                            buckets.RemoveAt(i--);
                    }
                }
            }
            f.Close();

            { var f2 = new StreamWriter(nom_fitxer_log, true); f2.WriteLine("Buckets final sense festius: " + buckets.Count); f2.Close(); }
        }

        private Dictionary<string, HashSet<Bucket>> ServeisBuckets(List<Bucket> buckets)
        {
            Dictionary<string, HashSet<Bucket>> serveis_buckets = new Dictionary<string, HashSet<Bucket>>();
            foreach (var s in llista_serveis)
            {
                bool trobat = false;
                foreach (var b in buckets)
                {
                    bool te_cp = b.ConteCP(s.CodiPostal);
                    if (te_cp)
                    {
                        if (!serveis_buckets.ContainsKey(s.Identificador)) serveis_buckets.Add(s.Identificador, new HashSet<Bucket>());
                        serveis_buckets[s.Identificador].Add(b);
                        trobat = true;
                    }
                }
                if (!trobat)
                {
                    // Un servei que no té cap bucket amb el seu CP -> li assigno el més proper
                    serveis_buckets.Add(s.Identificador, new HashSet<Bucket>());
                    double dist_min = 0;
                    foreach (var b in buckets)
                    {
                        double dist = b.DistanciaServei(s);
                        if (serveis_buckets[s.Identificador].Count == 0)
                        {
                            serveis_buckets[s.Identificador].Add(b);
                            dist_min = dist;
                        }
                        else if (dist == dist_min)
                        {
                            serveis_buckets[s.Identificador].Add(b);
                        }
                        else if (dist < dist_min)
                        {
                            serveis_buckets[s.Identificador].Clear();
                            serveis_buckets[s.Identificador].Add(b);
                            dist_min = dist;
                        }
                    }
                }
            }
            return serveis_buckets;
        }

        private void CrearLlistesServeis(bool nomes_els_que_toquen = false)
        {
            llista_serveis = new List<Servei>();
            llista_serveis_pou = new List<Servei>();
            foreach (var s in Context.Projecte.Serveis.Values)
            {
                if (!s.Planificar) continue;
                if (s.Frequencia.TotalDays == 0) continue;

                if (nomes_els_que_toquen)
                {
                    string zona_servei = Context.AssignarZona(s);
                    List<Tuple<DateTime, int>> dies;
                    DateTime inici = ConfigClient.DataInici;
                    if (s.UltimaVisita < ConfigClient.DataInici.AddDays((int)(-s.Frequencia.TotalDays * 7 / 5)))
                        inici = s.UltimaVisita;
                    Context.PlanificarVisitesServeiComplet(s, s.UltimaVisita, inici, ConfigClient.DataFi, out dies);
                    for (int id = 0; id < dies.Count; id++)
                        if (dies[id].Item1 < ConfigClient.DataInici)
                            dies.RemoveAt(id--);
                    Dictionary<int, int> ns = new Dictionary<int, int>();
                    foreach (var t in dies) if (!ns.ContainsKey(t.Item2)) ns.Add(t.Item2, 1); else ns[t.Item2]++;
                    if (ns.ContainsKey(1) && !ns.ContainsKey(0) && !ns.ContainsKey(2) && !ns.ContainsKey(3) && !ns.ContainsKey(4)) continue;
                }

                if (s.Frequencia.TotalDays >= 120)
                    llista_serveis_pou.Add(s);
                else
                    llista_serveis.Add(s);
            }
        }

        private void CrearMatriu(DateTime inici, DateTime fi)
        {
            matriu = new Dictionary<string, Dictionary<DateTime, int>>();
            matriu_inversa = new Dictionary<DateTime, Dictionary<string, int>>();
            foreach (var s in llista_serveis)
            {
                List<Tuple<DateTime, int>> dies;
                Context.PlanificarVisitesServeiComplet(s, s.UltimaVisita, inici, fi, out dies);
                matriu.Add(s.Identificador, new Dictionary<DateTime, int>());
                foreach (var tup in dies) matriu[s.Identificador].Add(tup.Item1, tup.Item2);
                foreach (var tup in dies)
                {
                    if (!matriu_inversa.ContainsKey(tup.Item1)) matriu_inversa.Add(tup.Item1, new Dictionary<string, int>());
                    matriu_inversa[tup.Item1].Add(s.Identificador, tup.Item2);
                }
            }
        }

        Dictionary<Bucket, Dictionary<DateTime, double>> puntuacions_precalculades;
        private double PuntuacioBucketDia(DateTime dia, Bucket b)
        {
            if (puntuacions_precalculades.ContainsKey(b) && puntuacions_precalculades[b].ContainsKey(dia))
                return puntuacions_precalculades[b][dia];

            double bondat_bucket = 0;
            foreach (var s in matriu_inversa[dia].Keys)
                if (serveis_buckets[s].Contains(b))
                {
                    Servei ser = Context.Projecte.Serveis[s];
                    int n_vs = 0; for (int i = 0; i < ser.Preferencies.Length; i++) if (ser.Preferencies[i] == 'V') n_vs++;

                    switch (matriu_inversa[dia][s])
                    {
                        case 4:
                            bondat_bucket += 300;
                            break;
                        case 3:
                            bondat_bucket += 20;
                            if (n_vs < 5) bondat_bucket += 10.0 * (5 - n_vs);
                            break;
                        case 2:
                            bondat_bucket += 10;
                            if (n_vs < 5) bondat_bucket += 1.0 * (5 - n_vs);
                            break;
                        case 1:
                            int indexdia = 0;
                            switch (dia.DayOfWeek)
                            {
                                case DayOfWeek.Monday: indexdia = 0; break;
                                case DayOfWeek.Tuesday: indexdia = 1; break;
                                case DayOfWeek.Wednesday: indexdia = 2; break;
                                case DayOfWeek.Thursday: indexdia = 3; break;
                                case DayOfWeek.Friday: indexdia = 4; break;
                                case DayOfWeek.Saturday: indexdia = 5; break;
                            }
                            if (indexdia < ser.Preferencies.Length && (ser.Preferencies[indexdia] == 'T' || ser.Preferencies[indexdia] == 'X'))
                            {
                                if (ser.Frequencia.TotalDays <= 5) bondat_bucket -= 500;
                                else if (ser.Frequencia.TotalDays <= 10) bondat_bucket -= 100;
                                else if (ser.Frequencia.TotalDays <= 20) bondat_bucket -= 10;
                                else bondat_bucket -= 1;
                                //if (ser.Frequencia.TotalDays <= 20) bondat_bucket -= 500;
                                //else if (ser.Frequencia.TotalDays <= 40) bondat_bucket -= 200;
                                //else bondat_bucket -= 100;
                            }
                            break;
                    }
                }
            if (!puntuacions_precalculades.ContainsKey(b)) puntuacions_precalculades.Add(b, new Dictionary<DateTime, double>());
            if (!puntuacions_precalculades[b].ContainsKey(dia)) puntuacions_precalculades[b].Add(dia, bondat_bucket);
            return bondat_bucket;
        }

        void EliminarGermansFalsos()
        {
            foreach (var b in buckets)
            {
                if (b.bucketB == null) continue;
                bool correcte = false;
                foreach (var cp in b.cpAssignats)
                    if (b.bucketB.ConteCP(cp.Id))
                    {
                        correcte = true;
                        break;
                    }
                if (correcte) continue;
                if (b.DistanciaBucketCP(b.bucketB) < 50) continue;
                b.bucketB.bucketB = null;
                b.bucketB = null;
            }
        }

        void FerGermans(string cp1, string cp2)
        {
            foreach (var b in buckets)
                if (b.ConteCP(cp1))
                    if (b.bucketB != null && !b.bucketB.ConteCP(cp2))
                        foreach (var b2 in buckets)
                            if (b2 != b && b2.ConteCP(cp2))
                                if (b2.bucketB != null && !b2.bucketB.ConteCP(cp1))
                                {
                                    b2.bucketB.bucketB = b.bucketB;
                                    b.bucketB.bucketB = b2.bucketB;
                                    b.bucketB = b2;
                                    b2.bucketB = b;
                                }
            foreach (var b in buckets)
                if (b.ConteCP(cp2))
                    if (b.bucketB != null && !b.bucketB.ConteCP(cp1))
                        foreach (var b2 in buckets)
                            if (b2 != b && b2.ConteCP(cp1))
                                if (b2.bucketB != null && !b2.bucketB.ConteCP(cp2))
                                {
                                    b2.bucketB.bucketB = b.bucketB;
                                    b.bucketB.bucketB = b2.bucketB;
                                    b.bucketB = b2;
                                    b2.bucketB = b;
                                }
        }

        private Dictionary<string, HashSet<Bucket>> AssignarDiesBuckets(int ndies)
        {
            //EliminarGermansFalsos();
            //FerGermans("08500", "08572");

            puntuacions_precalculades = new Dictionary<Bucket, Dictionary<DateTime, double>>();

            // Calcular màxim de vehicles (i per tant, buckets) per dia
            var max_vehicles_dia = new Dictionary<string, int>();
            int total_buckets = 0;
            foreach (var d in matriu_inversa.Keys)
            {
                int n = 0;
                foreach (var v in Context.Projecte.Vehicles.Values)
                {
                    bool fer = true;
                    if (d.DayOfWeek == DayOfWeek.Sunday) fer = false;
                    if (d.DayOfWeek == DayOfWeek.Saturday && v.Dissabtes != "SI") fer = false;
                    if (d >= v.IniciVacances && d <= v.FiVacances) fer = false;
                    if (v.Competencies.Contains("ReqPOU")) fer = false;
                    if (fer)
                    {
                        n++;
                        total_buckets++;
                    }
                }
                max_vehicles_dia.Add(d.ToShortDateString(), n);
            }

            { StreamWriter f = new StreamWriter(nom_fitxer_log, true); f.WriteLine("Buckets: " + buckets.Count + ". Total vehicles disponibles: " + total_buckets); f.Close(); }

            double millor_puntuacio = 0;
            Dictionary<string, HashSet<Bucket>> millor_buckets_dies = null;
            Dictionary<Bucket, DateTime?> millors_dies = null;
            int max_iter = 10000;
            DateTime inici = DateTime.Now;

            // Repetim el procés n vegades i ens quedem amb la millor
            string millor_assignacions_buckets = "";
            for (int iter = 0; iter < max_iter; iter++)
            {
                // Inicialitzem
                foreach (var b in buckets)
                    b.Dia = null;
                double puntuacio_acumulada = 0;
                string assignacions_buckets = "";

                var buckets_dies = new Dictionary<string, HashSet<Bucket>>();
                // Primer assignem els buckets setmanals, i finalment els quinzenals
                //List<Bucket> buckets_copia_no15 = new List<Bucket>();
                //List<Bucket> buckets_copia_15 = new List<Bucket>();
                //foreach (var b in buckets) if (b.bucketB == null) buckets_copia_15.Add(b); else buckets_copia_no15.Add(b);
                List<Bucket> buckets_copia = new List<Bucket>();
                foreach (var b in buckets) buckets_copia.Add(b);

                Dictionary<string, List<Bucket>> zones_buckets = new Dictionary<string, List<Bucket>>();
                foreach (var b in buckets) { if (!zones_buckets.ContainsKey(b.zona)) zones_buckets.Add(b.zona, new List<Bucket>()); zones_buckets[b.zona].Add(b); }

                //while (buckets_copia_no15.Count + buckets_copia_15.Count > 0)
                while (buckets_copia.Count > 0)
                {
                    assignacions_buckets += "Buckets copia: " + buckets_copia.Count + Environment.NewLine;
                    if (buckets.Count - buckets_copia.Count >= total_buckets) break;
                    // Comencem pels buckets de zones que s'hi hagi d'anar tots els dies de la setmana
                    var buckets_copia_zona = new Dictionary<string, List<Bucket>>();
                    List<Bucket> lzonamax = null;
                    //var bc = buckets_copia_no15; if (bc.Count == 0) bc = buckets_copia_15;
                    var bc = buckets_copia;
                    foreach (var b in bc)
                    {
                        if (!buckets_copia_zona.ContainsKey(b.zona)) buckets_copia_zona.Add(b.zona, new List<Bucket>());
                        buckets_copia_zona[b.zona].Add(b);
                        if (lzonamax == null || lzonamax.Count < buckets_copia_zona[b.zona].Count) lzonamax = buckets_copia_zona[b.zona];
                    }
                    int nbuckets_assignar = (int)Math.Floor((double)lzonamax.Count / ndies) * ndies;
                    if (nbuckets_assignar == 0)
                        nbuckets_assignar = lzonamax.Count;
                    int nivell = 0;
                    int assignats = 0;
                    bool algun = false;
                    while (lzonamax.Count > 0)
                    {
                        assignacions_buckets += "Zonamax: " + lzonamax.First().zona + Environment.NewLine;
                        // Primer buckets amb dia b, després els altres
                        //var lz2 = new List<Bucket>(); lz2.AddRange(lzonamax);
                        //lzonamax.Clear(); foreach (var b in lz2) if (b.bucketB != null) lzonamax.Add(b);
                        //if (lzonamax.Count == 0) foreach (var b in lz2) if (b.bucketB == null) lzonamax.Add(b);

                        algun = false;
                        foreach (var dia in matriu_inversa.Keys)
                        {
                            if (buckets_dies.ContainsKey(dia.ToShortDateString()) && buckets_dies[dia.ToShortDateString()].Count >= max_vehicles_dia[dia.ToShortDateString()]) continue;
                            if (max_vehicles_dia[dia.ToShortDateString()] == 0) continue;
                            int nzona = 0;
                            if (buckets_dies.ContainsKey(dia.ToShortDateString())) foreach (var bd in buckets_dies[dia.ToShortDateString()]) if (bd.zona == lzonamax[0].zona) nzona++;
                            if (nzona > nivell) continue;
                            algun = true;
                        }
                        assignacions_buckets += "Algun: " + algun + Environment.NewLine;
                        if (!algun) break;
                        foreach (var dia in matriu_inversa.Keys)
                        {
                            if (lzonamax.Count == 0) break;

                            // Comprovar que el dia no estigui complet
                            if (buckets_dies.ContainsKey(dia.ToShortDateString()) && buckets_dies[dia.ToShortDateString()].Count >= max_vehicles_dia[dia.ToShortDateString()]) continue;
                            if (max_vehicles_dia[dia.ToShortDateString()] == 0) continue;

                            // No assignar més d'un bucket per dia i nivell
                            int nzona = 0;
                            if (buckets_dies.ContainsKey(dia.ToShortDateString())) foreach (var bd in buckets_dies[dia.ToShortDateString()]) if (bd.zona == lzonamax[0].zona) nzona++;
                            if (nzona > nivell) continue;

                            var bondats_buckets = new List<Tuple<Bucket, double>>();
                            foreach (var b in lzonamax)
                            {
                                double bondat_bucket = 0;

                                // Penalitzar si ja n'hi ha un de la mateixa zona
                                bool impossible = false;
                                if (buckets_dies.ContainsKey(dia.ToShortDateString()))
                                    foreach (var bd in buckets_dies[dia.ToShortDateString()])
                                        if (bd.zona == b.zona)
                                        {
                                            //bondat_bucket -= 1000;
                                            //if (zones_buckets[bd.zona].Count / 10.0 <= 1) bondat_bucket -= 10000;
                                            bondat_bucket -= 100 - bd.DistanciaBucket(b);
                                            foreach (var s1 in bd.cpAssignats)
                                            {
                                                foreach (var s2 in b.cpAssignats)
                                                    if (s1.Id == s2.Id)
                                                    {
                                                        bondat_bucket -= 10000000;
                                                        //impossible = true;
                                                        //break;
                                                    }
                                                if (impossible) break;
                                            }
                                            if (impossible) break;
                                        }
                                if (impossible) continue;

                                // Penalitzar si ja n'hi ha un del seu mateix vehicle preferent
                                string vehicle_pref1 = "";
                                if (b.vehiclesPreasignats != null && b.vehiclesPreasignats.Count > 0) vehicle_pref1 = b.vehiclesPreasignats[0];
                                if (vehicle_pref1.Length > 0)
                                    if (buckets_dies.ContainsKey(dia.ToShortDateString()))
                                        foreach (var bd in buckets_dies[dia.ToShortDateString()])
                                        {
                                            string vehicle_pref2 = "";
                                            if (bd.vehiclesPreasignats != null && bd.vehiclesPreasignats.Count > 0) vehicle_pref2 = bd.vehiclesPreasignats[0];
                                            if (vehicle_pref2.Length > 0 && vehicle_pref1 == vehicle_pref2)
                                                bondat_bucket -= 10000;
                                        }

                                // Penalitzar si ja hi ha un de la mateiza zona en el dia que s'assignaria el seu bucket germà
                                if (b.bucketB != null && !b.bucketB.Dia.HasValue)
                                {
                                    DateTime dia_germa = dia.AddDays(7);
                                    if (!matriu_inversa.Keys.Contains(dia_germa)) dia_germa = dia.AddDays(-7);
                                    if (buckets_dies.ContainsKey(dia_germa.ToShortDateString()))
                                        foreach (var bd in buckets_dies[dia_germa.ToShortDateString()])
                                        {
                                            bondat_bucket -= 900;
                                            if (zones_buckets[bd.zona].Count / 10.0 <= 1) bondat_bucket -= 9000;
                                            bondat_bucket -= 100 - bd.DistanciaBucket(b);
                                            foreach (var s1 in bd.cpAssignats)
                                            {
                                                foreach (var s2 in b.cpAssignats)
                                                    if (s1.Id == s2.Id)
                                                    {
                                                        bondat_bucket -= 10000000;
                                                        //impossible = true;
                                                        //break;
                                                    }
                                                if (impossible) break;
                                            }
                                            if (impossible) break;
                                        }
                                    if (impossible) continue;

                                    // Penalitzar si ja n'hi ha un del seu mateix vehicle preferent en el dia que s'assignaria el seu bucket germà
                                    if (vehicle_pref1.Length > 0)
                                        if (buckets_dies.ContainsKey(dia_germa.ToShortDateString()))
                                            foreach (var bd in buckets_dies[dia_germa.ToShortDateString()])
                                                if (bd.zona == b.zona)
                                                {
                                                    string vehicle_pref2 = "";
                                                    if (bd.vehiclesPreasignats != null && bd.vehiclesPreasignats.Count > 0) vehicle_pref2 = bd.vehiclesPreasignats[0];
                                                    if (vehicle_pref2.Length > 0 && vehicle_pref1 == vehicle_pref2)
                                                        bondat_bucket -= 9000;
                                                }
                                }

                                //if (b.ConteCP("0882001") && dia.DayOfWeek == DayOfWeek.Monday)
                                //{
                                //    bondat_bucket -= 10000;
                                //}

                                // Puntuar segons les preferències dels serveis
                                bondat_bucket += PuntuacioBucketDia(dia, b);

                                // Puntuar segons les preferències del bucket germà
                                if (b.bucketB != null && !b.bucketB.Dia.HasValue)
                                {
                                    DateTime dia_germa = dia.AddDays(7);
                                    if (!matriu_inversa.Keys.Contains(dia_germa)) dia_germa = dia.AddDays(-7);
                                    bondat_bucket += PuntuacioBucketDia(dia_germa, b.bucketB);
                                }

                                bondats_buckets.Add(new Tuple<Bucket, double>(b, bondat_bucket));
                            }
                            if (bondats_buckets.Count > 0)
                            {
                                bondats_buckets.Sort(new Comparison<Tuple<Bucket, double>>((a1, b1) => Math.Sign(b1.Item2 - a1.Item2)));

                                int index = 0;
                                int N = 2;
                                if (iter > max_iter * 5 / 10) N = 3;
                                if (iter > max_iter * 7 / 10) N = 5;
                                if (iter > max_iter * 8 / 10) N = 8;
                                if (iter > max_iter * 9 / 10) N = 20;
                                if (iter > 0) index = r.Next(0, Math.Min(N, bondats_buckets.Count));
                                var millor = bondats_buckets[index];
                                millor.Item1.Dia = dia;
                                if (!buckets_dies.ContainsKey(dia.ToShortDateString())) buckets_dies.Add(dia.ToShortDateString(), new HashSet<Bucket>());
                                buckets_dies[dia.ToShortDateString()].Add(millor.Item1);
                                bc.Remove(millor.Item1);
                                lzonamax.Remove(millor.Item1);
                                assignats++;
                                puntuacio_acumulada += millor.Item2;

                                string scps = ""; foreach (var cp in millor.Item1.cpAssignats) scps += cp.Id + " ";
                                string poss = ""; foreach (var p in millor.Item1.posicion) poss += p;
                                assignacions_buckets += millor.Item1.zona + " (" + scps.Trim() + ")[" + poss + "] " + dia.ToShortDateString() + " [" + Math.Round(millor.Item2) + "]";

                                // Assignar bucket germà
                                var bucketb = millor.Item1.bucketB;
                                if (bucketb != null && !bucketb.Dia.HasValue)
                                {
                                    bool ok = true;

                                    DateTime dia_germa = millor.Item1.Dia.Value.AddDays(7);
                                    if (!matriu_inversa.Keys.Contains(dia_germa)) dia_germa = millor.Item1.Dia.Value.AddDays(-7);

                                    if (!matriu_inversa.Keys.Contains(dia_germa))
                                        ok = false;

                                    if (buckets_dies.ContainsKey(dia_germa.ToShortDateString()) && buckets_dies[dia_germa.ToShortDateString()].Count >= max_vehicles_dia[dia_germa.ToShortDateString()])
                                        ok = false;

                                    if (ok)
                                    {
                                        bucketb.Dia = dia_germa;
                                        if (!buckets_dies.ContainsKey(bucketb.Dia.Value.ToShortDateString())) buckets_dies.Add(bucketb.Dia.Value.ToShortDateString(), new HashSet<Bucket>());
                                        buckets_dies[bucketb.Dia.Value.ToShortDateString()].Add(bucketb);
                                        bc.Remove(bucketb);
                                        lzonamax.Remove(bucketb);
                                        assignats++;
                                        //puntuacio_acumulada += PuntuacioBucketDia(dia_germa, bucketb);
                                        //puntuacio_acumulada += millor.Item2;

                                        scps = ""; foreach (var cp in bucketb.cpAssignats) scps += cp.Id + " ";
                                        poss = ""; foreach (var p in bucketb.posicion) poss += p;
                                        assignacions_buckets += " -> Germà " + bucketb.zona + " (" + scps.Trim() + ")*[" + poss + "] " + dia_germa.ToShortDateString();
                                    }
                                    else
                                    {
                                        assignacions_buckets += " -> Germà KO";
                                        puntuacio_acumulada -= millor.Item2 / 2;
                                    }
                                }
                                assignacions_buckets += Environment.NewLine;
                            }
                            else break;
                        }
                        nivell++;
                        if (assignats >= nbuckets_assignar) break;
                    }
                    if (!algun) break;
                }

                if (iter == 0 || puntuacio_acumulada > millor_puntuacio)
                {
                    millor_assignacions_buckets = assignacions_buckets;
                    StreamWriter f = new StreamWriter(nom_fitxer_log, true); f.WriteLine("iter " + iter + " puntuació " + puntuacio_acumulada + " temps " + (DateTime.Now - inici).TotalSeconds); f.Close();
                    int n_serveis_cp_fora_dia = 0;
                    foreach (var s in llista_serveis)
                    {
                        foreach (var d in buckets_dies.Keys)
                        {
                            foreach (var b in buckets_dies[d])
                            {
                                if (b.ConteCP(s.CodiPostal))
                                {
                                    if (s.Preferencies != null && s.Preferencies.Length > 0)
                                    {
                                        int indexdia = 0;
                                        switch (DateTime.Parse(d).DayOfWeek)
                                        {
                                            case DayOfWeek.Monday: indexdia = 0; break;
                                            case DayOfWeek.Tuesday: indexdia = 1; break;
                                            case DayOfWeek.Wednesday: indexdia = 2; break;
                                            case DayOfWeek.Thursday: indexdia = 3; break;
                                            case DayOfWeek.Friday: indexdia = 4; break;
                                            case DayOfWeek.Saturday: indexdia = 5; break;
                                        }
                                        if (indexdia < s.Preferencies.Length && (s.Preferencies[indexdia] == 'T' || s.Preferencies[indexdia] == 'X'))
                                        {
                                            n_serveis_cp_fora_dia++;
                                        }
                                    }
                                }
                            }
                        }
                    }
                    f = new StreamWriter(nom_fitxer_log, true); f.WriteLine("Serveis en buckets de mal dia: " + n_serveis_cp_fora_dia); f.Close();
                    millor_puntuacio = puntuacio_acumulada;
                    millor_buckets_dies = buckets_dies;
                    millors_dies = new Dictionary<Bucket, DateTime?>();
                    foreach (var b in buckets)
                        millors_dies.Add(b, b.Dia);
                }
            }

            StreamWriter ff = new StreamWriter(nom_fitxer_log, true); ff.WriteLine(Environment.NewLine + millor_assignacions_buckets); ff.Close();

            foreach (var b in buckets)
                b.Dia = millors_dies[b];

            for (int i = 0; i < buckets.Count; i++)
                if (!buckets[i].Dia.HasValue)
                    buckets.RemoveAt(i--);

            { var f = new StreamWriter(nom_fitxer_log, true); f.WriteLine("N Buckets Final: " + buckets.Count); f.Close(); }

            return millor_buckets_dies;
        }


        private void PlanificacionsPOU()
        {
            // Creem llistat de vehicles
            var nvehicles = new List<int>();
            List<Vehicle> vehiclespou = new List<Vehicle>();
            foreach (var v in Context.Projecte.Vehicles)
            {
                if (v.Value.Competencies.Contains("ReqPOU")) vehiclespou.Add(v.Value);
            }
            var lvehiclespou = new Dictionary<string, List<Vehicle>>();
            for (DateTime da = ConfigClient.DataInici.Date; da <= ConfigClient.DataFi.Date; da = da.AddDays(1))
            {
                int n = 0;
                var l = new List<Vehicle>();
                foreach (var v in vehiclespou)
                {
                    bool fer = true;
                    if (da.DayOfWeek == DayOfWeek.Sunday) fer = false;
                    if (da.DayOfWeek == DayOfWeek.Saturday && v.Dissabtes != "SI") fer = false;
                    if (da >= v.IniciVacances && da <= v.FiVacances) fer = false;
                    if (fer)
                    {
                        n++;
                        l.Add(v);
                    }
                }
                lvehiclespou.Add(da.ToShortDateString(), l);
                nvehicles.Add(n);
            }

            // Executar classificador dies
            ClassificadorDies td = new ClassificadorDies(llista_serveis_pou, ConfigClient.DataInici.Date, ConfigClient.DataFi.Date, nvehicles);
            td.Executar();
            var dicDies = td.RetornaClassificacio();

            // Una planificació per cada dia
            foreach (DateTime dia in dicDies.Keys)
            {
                PlanificacioDia p = new PlanificacioDia();
                p.Dia = dia;
                p.Dades = new Projecte();
                p.Dades.AdrecesMercator = Context.Projecte.AdrecesMercator;
                p.Dades.Configuracio = Context.Projecte.Configuracio;
                p.Dades.Distancies = new Distancies(p.Dades, false);
                p.Dades.Serveis.Clear();
                foreach (Servei s in dicDies[dia]) p.Dades.Serveis.Add(s.Identificador, s);
                p.Dades.Vehicles.Clear();
                foreach (var v in lvehiclespou[dia.ToShortDateString()]) p.Dades.Vehicles.Add(v.Identificador, v);
                llista_planificacions.Add(p);
                if (!dicDiesF.ContainsKey(dia)) dicDiesF.Add(dia, new List<Servei>());
                dicDiesF[dia].AddRange(dicDies[dia]);
            }
        }

        private void Planificacions()
        {
            // Una planificació per cada bucket
            var planificacions = new Dictionary<string, List<Servei>>();
            var nbuckets = new Dictionary<string, int>();
            int nb = 0;
            foreach (var b in buckets)
            {
                nb++;
                string clau = nb + ";" + b.Dia.Value.ToShortDateString() + ";" + b.zona;
                //if (b.zona == "ZONA10") clau = "-1;" + b.Dia.Value.ToShortDateString() + ";" + b.zona;
                if (!planificacions.ContainsKey(clau))
                {
                    planificacions.Add(clau, new List<Servei>());
                    nbuckets.Add(clau, 0);
                }
                planificacions[clau].AddRange(b.serveis);
                nbuckets[clau]++;
            }

            // Assignem vehicles a planificacions
            var vehicles_zonadia = new Dictionary<string, List<Vehicle>>();
            AssignarVehiclesAPlanificacions(planificacions, nbuckets, vehicles_zonadia, true);

            // Creem llista de planifiacacions
            foreach (var z in planificacions)
            {
                var serveis = z.Value;
                var zona = z.Key.Split(';')[2];
                var diad = z.Key.Split(';')[1];
                var vehicles = vehicles_zonadia[z.Key];
                DateTime dia = DateTime.Parse(diad);

                // Llista de vehicles per dia
                var lvehicles = new List<Vehicle>();
                foreach (var v in vehicles)
                {
                    bool fer = true;
                    if (dia.DayOfWeek == DayOfWeek.Sunday) fer = false;
                    if (dia.DayOfWeek == DayOfWeek.Saturday && v.Dissabtes != "SI") fer = false;
                    if (dia >= v.IniciVacances && dia <= v.FiVacances) fer = false;
                    if (fer)
                        lvehicles.Add(v);
                }

                PlanificacioDia p = new PlanificacioDia();
                p.Dia = dia;
                p.Dades = new Projecte();
                p.Dades.AdrecesMercator = Context.Projecte.AdrecesMercator;
                p.Dades.Configuracio = Context.Projecte.Configuracio;
                p.Dades.Distancies = new Distancies(p.Dades, false);
                foreach (Servei s in serveis) p.Dades.Serveis.Add(s.Identificador, s);
                foreach (var v in lvehicles) p.Dades.Vehicles.Add(v.Identificador, v.FerCopia());
                llista_planificacions.Add(p);
                if (!dicDiesF.ContainsKey(dia)) dicDiesF.Add(dia, new List<Servei>());
                dicDiesF[dia].AddRange(serveis);
            }
        }

        private void PlanificacionsPerZona()
        {
            // Una planificació per cada zona i dia, amb tots els buckets que tingui
            var planificacions = new Dictionary<string, List<Servei>>();
            var nbuckets = new Dictionary<string, int>();
            foreach (var b in buckets)
            {
                string clau = b.Dia.Value.ToShortDateString() + ";" + b.zona;
                if (!planificacions.ContainsKey(clau))
                {
                    planificacions.Add(clau, new List<Servei>());
                    nbuckets.Add(clau, 0);
                }
                planificacions[clau].AddRange(b.serveis);
                nbuckets[clau]++;
            }

            // Assignem vehicles a planificacions
            var vehicles_zonadia = new Dictionary<string, List<Vehicle>>();
            AssignarVehiclesAPlanificacions(planificacions, nbuckets, vehicles_zonadia);

            // Creem llista de planifiacacions
            foreach (var z in planificacions)
            {
                var serveis = z.Value;
                var zona = z.Key.Split(';')[1];
                var diad = z.Key.Split(';')[0];
                var vehicles = vehicles_zonadia[z.Key];
                DateTime dia = DateTime.Parse(diad);

                // Llista de vehicles per dia
                var lvehicles = new List<Vehicle>();
                foreach (var v in vehicles)
                {
                    bool fer = true;
                    if (dia.DayOfWeek == DayOfWeek.Sunday) fer = false;
                    if (dia.DayOfWeek == DayOfWeek.Saturday && v.Dissabtes != "SI") fer = false;
                    if (dia >= v.IniciVacances && dia <= v.FiVacances) fer = false;
                    if (fer)
                        lvehicles.Add(v);
                }

                PlanificacioDia p = new PlanificacioDia();
                p.Dia = dia;
                p.Dades = new Projecte();
                p.Dades.AdrecesMercator = Context.Projecte.AdrecesMercator;
                p.Dades.Configuracio = Context.Projecte.Configuracio;
                p.Dades.Distancies = new Distancies(p.Dades, false);
                foreach (Servei s in serveis) p.Dades.Serveis.Add(s.Identificador, s);
                foreach (var v in lvehicles) p.Dades.Vehicles.Add(v.Identificador, v);
                llista_planificacions.Add(p);
                if (!dicDiesF.ContainsKey(dia)) dicDiesF.Add(dia, new List<Servei>());
                dicDiesF[dia].AddRange(serveis);
            }
        }

        private void AssignarVehiclesAPlanificacions(Dictionary<string, List<Servei>> planificacions, Dictionary<string, int> nbuckets, Dictionary<string, List<Vehicle>> vehicles_zonadia, bool vigilar_vacances = false)
        {
            var zones = Context.Zones();
            var vehicles_dia = new Dictionary<string, HashSet<string>>();
            string errors = "";

            // Primer de les zones que tenen serveis amb vehicle preferent
            /*var l = new List<Tuple<KeyValuePair<string, List<Servei>>, Dictionary<string, int>, double>>();
            foreach (var z in planificacions)
            {
                var zona = z.Key.Split(';')[2];
                var diad = z.Key.Split(';')[1];

                if (!vehicles_dia.ContainsKey(diad)) vehicles_dia.Add(diad, new HashSet<string>());
                if (!vehicles_zonadia.ContainsKey(z.Key)) vehicles_zonadia.Add(z.Key, new List<Vehicle>());
                var zz = zones[zona];

                var vehicles_preferents = new Dictionary<string, int>();
                double val = 0;
                foreach (var s in z.Value)
                {
                    if (s.VehiclesPreferents != null)
                        foreach (var idv in s.VehiclesPreferents.Split(';'))
                        {
                            var data = DateTime.Parse(diad);
                            if (Context.Projecte.Vehicles.ContainsKey(idv) && data >= Context.Projecte.Vehicles[idv].IniciVacances && data <= Context.Projecte.Vehicles[idv].FiVacances) continue;
                            if (!vehicles_preferents.ContainsKey(idv)) vehicles_preferents.Add(idv, 1);
                            else vehicles_preferents[idv]++;
                        }
                }
                foreach (var x in vehicles_preferents.Values) val += x;
                l.Add(new Tuple<KeyValuePair<string, List<Servei>>, Dictionary<string, int>, double>(z, vehicles_preferents, val));
            }
            l.Sort(new Comparison<Tuple<KeyValuePair<string, List<Servei>>, Dictionary<string, int>, double>>((a, b) => Math.Sign(b.Item3 - a.Item3)));
            for (int il = 0; il < l.Count; il++)
            {
                var z = l[il].Item1;
                var zona = z.Key.Split(';')[2];
                var diad = z.Key.Split(';')[1];
                var vehicles_preferents = l[il].Item2;
                var zz = zones[zona];

                if (vehicles_preferents.Count == 0) continue;

                List<Tuple<string, int>> lvehicles_preferents = new List<Tuple<string, int>>();
                foreach (var v in vehicles_preferents) lvehicles_preferents.Add(new Tuple<string, int>(v.Key, v.Value));
                lvehicles_preferents.Sort(new Comparison<Tuple<string, int>>((a, b) => Math.Sign(b.Item2 - a.Item2)));

                // Assignem els vehicles preferents dels serveis, vigilant de no repetir vehicles el mateix dia
                int vehicles_asignats = 0;
                for (int iv = 0; iv < lvehicles_preferents.Count; iv++)
                {
                    IDictionary<string, Vehicle> vehicles_disponibles = new Dictionary<string, Vehicle>();
                    foreach (var v in lvehicles_preferents)
                        if (!vehicles_dia[diad].Contains(v.Item1))
                        {
                            if (Context.Projecte.Vehicles.ContainsKey(v.Item1))
                            {
                                vehicles_disponibles.Add(v.Item1, Context.Projecte.Vehicles[v.Item1]);
                                break;
                            }
                            else
                            {
                                errors += "Servei amb vehicle preferent inexistent '" + v.Item1 + "', s'ignorarà." + Environment.NewLine;
                            }
                        }
                    if (vehicles_disponibles.Count > 0)
                    {
                        Vehicle vv = Context.AssignarVehicleMesProper(zz, vehicles_disponibles);
                        vehicles_dia[diad].Add(vv.Identificador);
                        vehicles_zonadia[z.Key].Add(vv);
                        vehicles_asignats++;
                        if (vehicles_asignats >= nbuckets[z.Key])
                            break;
                    }
                }
            }*/

            // Segon de les zones que tenen vehicles predefinits
            foreach (var z in planificacions)
            {
                var zona = z.Key.Split(';')[2];
                var diad = z.Key.Split(';')[1];
                if (!vehicles_dia.ContainsKey(diad)) vehicles_dia.Add(diad, new HashSet<string>());
                if (!vehicles_zonadia.ContainsKey(z.Key)) vehicles_zonadia.Add(z.Key, new List<Vehicle>());
                var zz = zones[zona];

                int vehicles_asignats = vehicles_zonadia[z.Key].Count;
                if (vehicles_asignats >= nbuckets[z.Key]) continue; // Ja està complet

                if (zz.Vehicles == null) continue;

                // Assignem els vehicles preferents de la zona, vigilant de no repetir vehicles el mateix dia
                for (int iv = 0; iv < zz.Vehicles.Count; iv++)
                {
                    IDictionary<string, Vehicle> vehicles_disponibles = new Dictionary<string, Vehicle>();
                    foreach (var idv in zz.Vehicles)
                        if (!vehicles_dia[diad].Contains(idv))
                        {
                            if (Context.Projecte.Vehicles.ContainsKey(idv))
                            {
                                var data = DateTime.Parse(diad);
                                if (data >= Context.Projecte.Vehicles[idv].IniciVacances && data <= Context.Projecte.Vehicles[idv].FiVacances) continue;
                                vehicles_disponibles.Add(idv, Context.Projecte.Vehicles[idv]);
                            }
                            else
                                errors += "Zona '" + z.Key + "' amb vehicle assignat inexistent '" + idv + "', s'ignorarà." + Environment.NewLine;
                        }
                    if (vehicles_disponibles.Count > 0)
                    {
                        Vehicle vv = Context.AssignarVehicleMesProper(zz, vehicles_disponibles);
                        vehicles_dia[diad].Add(vv.Identificador);
                        vehicles_zonadia[z.Key].Add(vv);
                        vehicles_asignats++;
                        if (vehicles_asignats >= nbuckets[z.Key])
                            break;
                    }
                }
            }

            // Finalment de les zones que queden
            var lplanificacions = new List<Tuple<string, List<Servei>, double>>();
            foreach (var z in planificacions)
            {
                var zona = z.Key.Split(';')[2];
                var diad = z.Key.Split(';')[1];
                if (!vehicles_dia.ContainsKey(diad)) vehicles_dia.Add(diad, new HashSet<string>());
                if (!vehicles_zonadia.ContainsKey(z.Key)) vehicles_zonadia.Add(z.Key, new List<Vehicle>());
                var zz = zones[zona];
                int vehicles_asignats = vehicles_zonadia[z.Key].Count;
                if (vehicles_asignats >= nbuckets[z.Key]) continue; // Ja està complet
                IDictionary<string, Vehicle> vehicles_disponibles = new Dictionary<string, Vehicle>();
                foreach (var idv in Context.Projecte.Vehicles.Keys)
                {
                    var data = DateTime.Parse(diad);
                    if (Context.Projecte.Vehicles.ContainsKey(idv) && data >= Context.Projecte.Vehicles[idv].IniciVacances && data <= Context.Projecte.Vehicles[idv].FiVacances) continue;
                    if (!vehicles_dia[diad].Contains(idv))
                        vehicles_disponibles.Add(idv, Context.Projecte.Vehicles[idv]);
                }
                double dd = -1;
                foreach (var v in vehicles_disponibles)
                {
                    var Base = v.Value.Origen;
                    if (v.Value.CasaRepartidor != null) Base = v.Value.CasaRepartidor;
                    double dd1 = ClassificadorDies.DistanciaKm(zz.Punt.Y, zz.Punt.X, Base.Y, Base.X);
                    if (dd < 0 || dd1 < dd)
                        dd = dd1;
                }
                lplanificacions.Add(new Tuple<string, List<Servei>, double>(z.Key, z.Value, dd));
            }
            lplanificacions.Sort(new Comparison<Tuple<string, List<Servei>, double>>((a, b) => Math.Sign(b.Item3 - a.Item3)));
            foreach (var z in lplanificacions)
            {
                var zona = z.Item1.Split(';')[2];
                var diad = z.Item1.Split(';')[1];
                if (!vehicles_dia.ContainsKey(diad)) vehicles_dia.Add(diad, new HashSet<string>());
                if (!vehicles_zonadia.ContainsKey(z.Item1)) vehicles_zonadia.Add(z.Item1, new List<Vehicle>());
                var zz = zones[zona];

                int vehicles_asignats = vehicles_zonadia[z.Item1].Count;
                if (vehicles_asignats >= nbuckets[z.Item1]) continue; // Ja està complet

                // Assignem un vehicle de tots els possibles, vigilant de no repetir vehicles el mateix dia
                for (int iv = 0; iv < Context.Projecte.Vehicles.Count; iv++)
                {
                    IDictionary<string, Vehicle> vehicles_disponibles = new Dictionary<string, Vehicle>();
                    foreach (var idv in Context.Projecte.Vehicles.Keys)
                    {
                        var data = DateTime.Parse(diad);
                        if (Context.Projecte.Vehicles.ContainsKey(idv) && data >= Context.Projecte.Vehicles[idv].IniciVacances && data <= Context.Projecte.Vehicles[idv].FiVacances) continue;
                        if (!vehicles_dia[diad].Contains(idv))
                            vehicles_disponibles.Add(idv, Context.Projecte.Vehicles[idv]);
                    }
                    if (vehicles_disponibles.Count > 0)
                    {
                        Vehicle vv = Context.AssignarVehicleMesProper(zz, vehicles_disponibles);
                        if (vv != null)
                        {
                            vehicles_dia[diad].Add(vv.Identificador);
                            vehicles_zonadia[z.Item1].Add(vv);
                        }
                        else
                        {
                            //MessageBox.Show(vehicles_disponibles.ToString());
                            break;
                        }
                        vehicles_asignats++;
                        if (vehicles_asignats >= nbuckets[z.Item1])
                            break;
                    }
                }
                if (vehicles_zonadia[z.Item1].Count != nbuckets[z.Item1])
                {
                    //MessageBox.Show("No he aconseguit assignar prous vehicles a la planificació " + z.Item1);
                }
            }

            if (errors.Length > 0)
            {
                var f = new StreamWriter(nom_fitxer_log, true); f.WriteLine(errors); f.Close();
                //MessageBox.Show(errors);
            }
        }

        #region Repartiments
        private int RepartirCarregaBucketsGermans()
        {
            int serevis_moguts = 0;
            Dictionary<string, List<Bucket>> dic_zones_buckets = new Dictionary<string, List<Bucket>>();
            foreach (var b in buckets) if (!dic_zones_buckets.ContainsKey(b.zona)) dic_zones_buckets.Add(b.zona, new List<Bucket>() { b }); else dic_zones_buckets[b.zona].Add(b);
            Dictionary<string, List<Bucket>> dic_cps = new Dictionary<string, List<Bucket>>();
            foreach (var b in buckets) foreach (var cp in b.cpAssignats) if (!dic_cps.ContainsKey(cp.Id)) dic_cps.Add(cp.Id, new List<Bucket>() { b }); else dic_cps[cp.Id].Add(b);

            foreach (var zona in dic_zones_buckets.Keys)
            {
                // Per cada zona, intentar repartir els buckets
                var l_buckets_zona = dic_zones_buckets[zona];
                l_buckets_zona.Sort(new Comparison<Bucket>((a, b) => Math.Sign(a.serveis.Count - b.serveis.Count)));
                if (l_buckets_zona.Count > 1)
                {
                    foreach (var b in l_buckets_zona)
                    {
                        if (b.bucketB != null)
                        {
                            HashSet<Servei> moguts = new HashSet<Servei>();
                            while (b.serveis.Count != b.bucketB.serveis.Count)
                            {
                                var gros = b;
                                var petit = b.bucketB;
                                if (petit.serveis.Count > gros.serveis.Count) { petit = b; gros = b.bucketB; }

                                var serveis_bgros = new List<Tuple<Servei, Bucket, double>>();
                                bool algun = false;
                                foreach (var sg in gros.serveis)
                                {
                                    bool hi_es = false;
                                    foreach (var bb in l_buckets_zona)
                                    {
                                        if (bb == gros) continue;
                                        if (bb.Dia == petit.Dia)
                                            foreach (var sp in bb.serveis) if (sp.Identificador == sg.Identificador) { hi_es = true; break; }
                                    }
                                    if (hi_es) continue;

                                    bool cptrobat = petit.ConteCP(sg.CodiPostal);
                                    if (!cptrobat) continue;

                                    var linia_matriu = matriu[sg.Identificador];
                                    bool puc_moure = false;

                                    if (linia_matriu[gros.Dia.Value] == 3 && linia_matriu[petit.Dia.Value] == 2 && Math.Abs((gros.Dia.Value - petit.Dia.Value).TotalDays) <= sg.Marge.TotalDays) puc_moure = true;
                                    else if (linia_matriu[gros.Dia.Value] == 2 && linia_matriu[petit.Dia.Value] >= 2 && Math.Abs((gros.Dia.Value - petit.Dia.Value).TotalDays) <= sg.Marge.TotalDays) puc_moure = true;

                                    if (puc_moure)
                                    {
                                        if (!moguts.Contains(sg) && petit.AssignarServei(sg, buckets) == 0)
                                        {
                                            moguts.Add(sg);
                                            serevis_moguts++;
                                            gros.serveis.Remove(sg);
                                            algun = true;
                                            break;
                                        }
                                    }
                                }
                                if (!algun) break;
                            }
                        }
                    }
                }
            }
            return serevis_moguts;
        }

        private int Tunning_ReduirKm_2(int maximaDistancia, bool intradia)
        {
            int serevis_moguts = 0;
            Dictionary<string, List<Bucket>> dic_zones_buckets = new Dictionary<string, List<Bucket>>();
            foreach (var b in buckets)
                if (!dic_zones_buckets.ContainsKey(b.zona))
                    dic_zones_buckets.Add(b.zona, new List<Bucket>() { b });
                else dic_zones_buckets[b.zona].Add(b);
            Dictionary<string, List<Bucket>> dic_cps = new Dictionary<string, List<Bucket>>();
            foreach (var b in buckets)
                foreach (var cp in b.cpAssignats)
                    if (!dic_cps.ContainsKey(cp.Id))
                        dic_cps.Add(cp.Id, new List<Bucket>() { b });
                    else dic_cps[cp.Id].Add(b);

            double max_dist = maximaDistancia;
            if (intradia)
                max_dist = 0;
            foreach (var zona in dic_zones_buckets.Keys)
            {
                var l_buckets_zona = dic_zones_buckets[zona];

                //for (int i = 0; i < l_buckets_zona.Count-1; i++)
                for (int i = 0; i < l_buckets_zona.Count; i++)
                {
                    Bucket BucketA = l_buckets_zona[i];

                    // for (int j = i+1; j < l_buckets_zona.Count; j++)
                    for (int j = 0; j < l_buckets_zona.Count; j++)
                    {
                        if (i != j)
                        {
                            Bucket BucketB = l_buckets_zona[j];

                            TimeSpan ts = (DateTime)BucketB.Dia - (DateTime)BucketA.Dia;
                            int dif = (int)Math.Abs(ts.TotalDays);
                            if (dif <= max_dist)
                            {
                                for (int s1 = 0; s1 < BucketA.serveis.Count; s1++)
                                {
                                    Servei sA = BucketA.serveis[s1];
                                    if (sA.VehiclesPreferents == null || sA.VehiclesPreferents.Length == 0)
                                    {
                                        if (intradia || (sA.Frequencia.TotalDays > 10 && sA.Preferencies == "VVVVVT"))
                                        {
                                            double distMinA = 1000000;
                                            foreach (Servei s in BucketA.serveis)
                                            {
                                                if (s != sA)
                                                {
                                                    double d = Context.DistanciaKm(s.Adreça.Y, s.Adreça.X, sA.Adreça.Y, sA.Adreça.X, true, false);

                                                    if (d < distMinA)
                                                        distMinA = d;
                                                }
                                            }

                                            double distMinB = 1000000;
                                            foreach (Servei s in BucketB.serveis)
                                            {
                                                if (s != sA)
                                                {
                                                    double d = Context.DistanciaKm(s.Adreça.Y, s.Adreça.X, sA.Adreça.Y, sA.Adreça.X, true, false);

                                                    if (d < distMinB)
                                                    {
                                                        distMinB = d;
                                                    }
                                                }
                                            }

                                            //bool Condicio1 = ((distMinB < distMinA) && (BucketA.serveis.Count > BucketB.serveis.Count));
                                            bool DistanciaMoltMenor = false;
                                            bool DistanciaSimplementMenor = false;
                                            bool DistanciaIgual = false;

                                            if (distMinB < (distMinA * 0.3))
                                                DistanciaMoltMenor = true;
                                            else
                                                if (distMinB < distMinA)
                                                {
                                                    DistanciaSimplementMenor = true;
                                                }
                                                else
                                                    if (distMinA == distMinB)
                                                    {
                                                        DistanciaIgual = true;
                                                    }

                                            bool CanviQueNoDesequilibra = (BucketA.serveis.Count + 4 > BucketB.serveis.Count);

                                            if (distMinB == 0)
                                                distMinA.ToString();

                                            if (DistanciaMoltMenor && CanviQueNoDesequilibra)
                                            {
                                                if (!BucketB.serveis.Contains(sA))
                                                {
                                                    bool trobat = false;
                                                    for (int y = 0; y < BucketB.serveis.Count; y++)
                                                    {
                                                        if (BucketB.serveis[y].Identificador == sA.Identificador)
                                                            trobat = true;
                                                    }
                                                    trobat.ToString();

                                                    BucketA.serveis.Remove(sA);
                                                    BucketB.serveis.Add(sA);
                                                    serevis_moguts++;
                                                    s1--;
                                                    continue;
                                                }
                                            }

                                            if (DistanciaMoltMenor && !CanviQueNoDesequilibra || DistanciaSimplementMenor)
                                            {
                                                // Es possible passar un servei del BucketB al BucketA ?
                                                Servei strobat = null;
                                                double distMinTotal = 1000000;
                                                foreach (Servei sbb in BucketB.serveis)
                                                {
                                                    if (intradia ||(sbb.Frequencia.TotalDays > 10 && sbb.Preferencies == "VVVVT"))
                                                    {
                                                        double distMinEntreBiB = 1000000;
                                                        foreach (Servei sbbb in BucketB.serveis)
                                                        {
                                                            if (sbb != sA && sbb != sbbb)
                                                            {
                                                                double d1 = Context.DistanciaKm(sbbb.Adreça.Y, sbbb.Adreça.X, sbb.Adreça.Y, sbb.Adreça.X, true, false);

                                                                if (d1 < distMinEntreBiB)
                                                                {
                                                                    distMinEntreBiB = d1;
                                                                    //strobat = sbb;
                                                                }
                                                            }
                                                        }

                                                        //Servei strobat2 = null;
                                                        double distMinEntreAiB = 1000000;
                                                        foreach (Servei saa in BucketA.serveis)
                                                        {
                                                            if (sbb != sA && sbb != saa)
                                                            {
                                                                double d1 = Context.DistanciaKm(saa.Adreça.Y, saa.Adreça.X, sbb.Adreça.Y, sbb.Adreça.X, true, false);

                                                                if (d1 < distMinEntreAiB)
                                                                {
                                                                    distMinEntreAiB = d1;
                                                                    //strobat2 = sbb;
                                                                }
                                                            }
                                                        }

                                                        if (distMinEntreAiB < distMinEntreBiB && distMinEntreAiB < distMinTotal)
                                                        {
                                                            strobat = sbb;
                                                            distMinTotal = distMinEntreAiB;
                                                        }
                                                    }
                                                }
                                                if (strobat != null)
                                                {
                                                    //fer intercanvi
                                                    BucketA.ToString();
                                                    strobat.ToString();

                                                    if (!BucketB.serveis.Contains(sA) && !BucketA.serveis.Contains(strobat))
                                                    {
                                                        if (MoureCorrecte(sA, BucketA.Dia.Value, BucketB.Dia.Value, false) && MoureCorrecte(strobat, BucketB.Dia.Value, BucketA.Dia.Value, false))
                                                        {
                                                            visites_planificades_serveis[sA.Identificador].Remove(BucketA.Dia.Value);
                                                            visites_planificades_serveis[sA.Identificador].Add(BucketB.Dia.Value);
                                                            visites_planificades_serveis[strobat.Identificador].Remove(BucketB.Dia.Value);
                                                            visites_planificades_serveis[strobat.Identificador].Add(BucketA.Dia.Value);

                                                            BucketA.serveis.Remove(sA);
                                                            BucketB.serveis.Add(sA);
                                                            serevis_moguts++;

                                                            BucketB.serveis.Remove(strobat);
                                                            BucketA.serveis.Add(strobat);
                                                            serevis_moguts++;
                                                            //s1--;
                                                            //continue;
                                                        }
                                                    }

                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            return serevis_moguts;
        }


        private int Tunning_ReduirKm_()
        {
            int serevis_moguts = 0;
            Dictionary<string, List<Bucket>> dic_zones_buckets = new Dictionary<string, List<Bucket>>();
            foreach (var b in buckets)
                if (!dic_zones_buckets.ContainsKey(b.zona))
                    dic_zones_buckets.Add(b.zona, new List<Bucket>() { b });
                else dic_zones_buckets[b.zona].Add(b);
            Dictionary<string, List<Bucket>> dic_cps = new Dictionary<string, List<Bucket>>();
            foreach (var b in buckets)
                foreach (var cp in b.cpAssignats)
                    if (!dic_cps.ContainsKey(cp.Id))
                        dic_cps.Add(cp.Id, new List<Bucket>() { b });
                    else dic_cps[cp.Id].Add(b);

            double max_dist = 0;
            foreach (var zona in dic_zones_buckets.Keys)
            {
                var l_buckets_zona = dic_zones_buckets[zona];

                //for (int i = 0; i < l_buckets_zona.Count-1; i++)
                for (int i = 0; i < l_buckets_zona.Count; i++)
                {
                    Bucket BucketA = l_buckets_zona[i];

                    //                    for (int j = i+1; j < l_buckets_zona.Count; j++)
                    for (int j = 0; j < l_buckets_zona.Count; j++)
                    {
                        if (i != j)
                        {
                            Bucket BucketB = l_buckets_zona[j];

                            TimeSpan ts = (DateTime)BucketB.Dia - (DateTime)BucketA.Dia;
                            int dif = (int)Math.Abs(ts.TotalDays);
                            if (dif <= max_dist)
                            {
                                for (int s1 = 0; s1 < BucketA.serveis.Count; s1++)
                                {
                                    Servei sA = BucketA.serveis[s1];
                                    if (sA.VehiclesPreferents == null || sA.VehiclesPreferents.Length == 0)
                                    {
                                        if (sA.Frequencia.TotalDays > 10 && sA.Preferencies == "VVVVVT")
                                        {

                                            double distMinA = 1000000;
                                            foreach (Servei s in BucketA.serveis)
                                            {
                                                if (s != sA)
                                                {
                                                    double d = Context.DistanciaKm(s.Adreça.Y, s.Adreça.X, sA.Adreça.Y, sA.Adreça.X, true, false);

                                                    if (d < distMinA)
                                                        distMinA = d;
                                                }
                                            }

                                            double distMinB = 1000000;
                                            foreach (Servei s in BucketB.serveis)
                                            {
                                                if (s != sA)
                                                {
                                                    double d = Context.DistanciaKm(s.Adreça.Y, s.Adreça.X, sA.Adreça.Y, sA.Adreça.X, true, false);

                                                    if (d < distMinB)
                                                    {
                                                        distMinB = d;
                                                    }
                                                }
                                            }

                                            //bool Condicio1 = ((distMinB < distMinA) && (BucketA.serveis.Count > BucketB.serveis.Count));
                                            bool Condicio2 = (distMinB < (distMinA * 0.5));
                                            bool Condicio3 = (BucketA.serveis.Count + 3 > BucketB.serveis.Count);

                                            if (Condicio2 && Condicio3)
                                            {
                                                if (!BucketB.serveis.Contains(sA))
                                                {
                                                    BucketA.serveis.Remove(sA);
                                                    BucketB.serveis.Add(sA);
                                                    serevis_moguts++;
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            return serevis_moguts;
        }

        private int Tunning_ReduirKm()
        {
            int serevis_moguts = 0;
            Dictionary<string, List<Bucket>> dic_zones_buckets = new Dictionary<string, List<Bucket>>();
            foreach (var b in buckets)
                if (!dic_zones_buckets.ContainsKey(b.zona))
                    dic_zones_buckets.Add(b.zona, new List<Bucket>() { b });
                else dic_zones_buckets[b.zona].Add(b);
            Dictionary<string, List<Bucket>> dic_cps = new Dictionary<string, List<Bucket>>();
            foreach (var b in buckets)
                foreach (var cp in b.cpAssignats)
                    if (!dic_cps.ContainsKey(cp.Id))
                        dic_cps.Add(cp.Id, new List<Bucket>() { b });
                    else dic_cps[cp.Id].Add(b);

            double max_dist = 4;
            foreach (var zona in dic_zones_buckets.Keys)
            {
                var l_buckets_zona = dic_zones_buckets[zona];

                //for (int i = 0; i < l_buckets_zona.Count-1; i++)
                for (int i = 0; i < l_buckets_zona.Count; i++)
                {
                    Bucket BucketA = l_buckets_zona[i];

                    //                    for (int j = i+1; j < l_buckets_zona.Count; j++)
                    for (int j = 0; j < l_buckets_zona.Count; j++)
                    {
                        if (i != j)
                        {
                            Bucket BucketB = l_buckets_zona[j];

                            TimeSpan ts = (DateTime)BucketB.Dia - (DateTime)BucketA.Dia;
                            int dif = (int)Math.Abs(ts.TotalDays);
                            if (dif < max_dist)
                            {
                                for (int s1 = 0; s1 < BucketA.serveis.Count; s1++)
                                {
                                    Servei sA = BucketA.serveis[s1];
                                    if (sA.VehiclesPreferents == null || sA.VehiclesPreferents.Length == 0)
                                    {
                                        if (sA.Frequencia.TotalDays > 10 && sA.Preferencies == "VVVVVT")
                                        {

                                            double distMinA = 1000000;
                                            foreach (Servei s in BucketA.serveis)
                                            {
                                                if (s != sA)
                                                {
                                                    double d = Context.DistanciaKm(s.Adreça.Y, s.Adreça.X, sA.Adreça.Y, sA.Adreça.X, true, false);

                                                    if (d < distMinA)
                                                        distMinA = d;
                                                }
                                            }

                                            double distMinB = 1000000;
                                            foreach (Servei s in BucketB.serveis)
                                            {
                                                if (s != sA)
                                                {
                                                    double d = Context.DistanciaKm(s.Adreça.Y, s.Adreça.X, sA.Adreça.Y, sA.Adreça.X, true, false);

                                                    if (d < distMinB)
                                                    {
                                                        distMinB = d;
                                                    }
                                                }
                                            }

                                            //bool Condicio1 = ((distMinB < distMinA) && (BucketA.serveis.Count > BucketB.serveis.Count));
                                            bool Condicio2 = (distMinB < (distMinA * 0.5));
                                            bool Condicio3 = (BucketA.serveis.Count + 3 > BucketB.serveis.Count);

                                            if (Condicio2 && Condicio3)
                                            {
                                                if (!BucketB.serveis.Contains(sA))
                                                {
                                                    BucketA.serveis.Remove(sA);
                                                    BucketB.serveis.Add(sA);
                                                    serevis_moguts++;
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            return serevis_moguts;
        }

        private int Tunning_Igualar(int maximaDistancia, bool intradia)
        {
            int serevis_moguts = 0;
            Dictionary<string, List<Bucket>> dic_zones_buckets = new Dictionary<string, List<Bucket>>();
            foreach (var b in buckets)
                if (!dic_zones_buckets.ContainsKey(b.zona))
                    dic_zones_buckets.Add(b.zona, new List<Bucket>() { b });
                else dic_zones_buckets[b.zona].Add(b);
            Dictionary<string, List<Bucket>> dic_cps = new Dictionary<string, List<Bucket>>();
            foreach (var b in buckets)
                foreach (var cp in b.cpAssignats)
                    if (!dic_cps.ContainsKey(cp.Id))
                        dic_cps.Add(cp.Id, new List<Bucket>() { b });
                    else dic_cps[cp.Id].Add(b);

            double max_dist = maximaDistancia;
            if (intradia) max_dist = 0;
            foreach (var zona in dic_zones_buckets.Keys)
            {
                var l_buckets_zona = dic_zones_buckets[zona];

                //for (int i = 0; i < l_buckets_zona.Count-1; i++)
                for (int i = 0; i < l_buckets_zona.Count; i++)
                {
                    Bucket BucketA = l_buckets_zona[i];
                    
//                    for (int j = i+1; j < l_buckets_zona.Count; j++)
                    for (int j = 0; j < l_buckets_zona.Count; j++)
                    {
                        if (i!=j)
                        {
                        Bucket BucketB = l_buckets_zona[j];

                        TimeSpan ts = (DateTime)BucketB.Dia - (DateTime)BucketA.Dia;
                        int dif = (int)Math.Abs(ts.TotalDays);
                        if (dif <= max_dist)
                        {
                            for (int s1 = 0; s1 < BucketA.serveis.Count; s1++)
                            {
                                Servei sA = BucketA.serveis[s1];
                                if (sA.VehiclesPreferents == null || sA.VehiclesPreferents.Length == 0)
                                {
                                    if (intradia || (sA.Frequencia.TotalDays > 10 && sA.Preferencies == "VVVVVT"))
                                    {

                                        double distMinA = 1000000;
                                        foreach (Servei s in BucketA.serveis)
                                        {
                                            if (s != sA)
                                            {
                                                double d = Context.DistanciaKm(s.Adreça.Y, s.Adreça.X, sA.Adreça.Y, sA.Adreça.X, true, false);

                                                if (d < distMinA)
                                                    distMinA = d;
                                            }
                                        }

                                        double distMinB = 1000000;
                                        foreach (Servei s in BucketB.serveis)
                                        {
                                            if (s != sA)
                                            {
                                                double d = Context.DistanciaKm(s.Adreça.Y, s.Adreça.X, sA.Adreça.Y, sA.Adreça.X, true, false);

                                                if (d < distMinB)
                                                {
                                                    distMinB = d;
                                                }
                                            }
                                        }

                                        //bool Condicio1 = ((distMinB < distMinA) && (BucketA.serveis.Count > BucketB.serveis.Count));
                                        bool Condicio2 = ((distMinB < (distMinA * 1.05)) && (BucketA.serveis.Count > BucketB.serveis.Count));

                                        if (Condicio2)
                                        {
                                            if (!BucketB.serveis.Contains(sA))
                                            {
                                                if (MoureCorrecte(sA, BucketA.Dia.Value, BucketB.Dia.Value))
                                                {
                                                    BucketA.serveis.Remove(sA);
                                                    BucketB.serveis.Add(sA);
                                                    serevis_moguts++;
                                                }
                                            }
                                        }

                                        //for (int s2 = 0; s2 < BucketB.serveis.Count; s2++)
                                        //{
                                        //    Servei sB = BucketB.serveis[s2];

                                        //    if (sB.Frequencia.TotalDays > 10 && sB.Preferencies == "VVVVVT")
                                        //    {

                                        //    }
                                        //}
                                    }
                                }
                            }
                        }
                        }
                    }
                }
            }
            return serevis_moguts;
        }

        private bool MoureCorrecte(Servei servei, DateTime data_vella, DateTime data_nova, bool actualitzar_llistes = true)
        {
            if (data_nova.Date == data_vella.Date) return true;

            string id = servei.Identificador;
            DateTime? anterior = null, posterior = null;
            var l = visites_planificades_serveis[id].ToList();
            l.Sort();
            for (int i = 0; i < l.Count; i++)
            {
                if (l[i].Date == data_vella.Date)
                {
                    if (i > 0) anterior = l[i - 1];
                    if (i + 1 < l.Count) posterior = l[i + 1];
                    break;
                }
            }
            var marge = servei.Marge.TotalDays / 2;
            if (anterior != null) if (Math.Abs(servei.Frequencia.TotalDays * 7 / 5 - (data_nova - anterior.Value).TotalDays) > marge) return false;
            if (posterior != null) if (Math.Abs(servei.Frequencia.TotalDays * 7 / 5 - (posterior.Value - data_nova).TotalDays) > marge) return false;

            if (actualitzar_llistes)
            {
                visites_planificades_serveis[id].Remove(data_vella);
                visites_planificades_serveis[id].Add(data_nova);
            }
            return true;
        }

        private int RepartirCarregaBucketsZona(HashSet<string> zones_no = null)
        {
            int serevis_moguts = 0;
            Dictionary<string, List<Bucket>> dic_zones_buckets = new Dictionary<string, List<Bucket>>();
            foreach (var b in buckets) 
                if (!dic_zones_buckets.ContainsKey(b.zona)) 
                    dic_zones_buckets.Add(b.zona, new List<Bucket>() { b }); 
                else dic_zones_buckets[b.zona].Add(b);

            double max_dist = 2;
            foreach (var zona in dic_zones_buckets.Keys)
            {
                if (zones_no != null && zones_no.Contains(zona)) continue;

                // Per cada zona, intentar repartir els buckets
                var l_buckets_zona = dic_zones_buckets[zona];
                if (l_buckets_zona.Count > 1)
                {
                    // Calculem saturació
                    int suma_serveis = 0;
                    foreach (var b in l_buckets_zona) 
                        suma_serveis += b.serveis.Count;
                    int saturacio = suma_serveis / l_buckets_zona.Count;

                    // Per cada bucket amb pocs serveis, intentem ficar-n'hi
                    l_buckets_zona.Sort(new Comparison<Bucket>((a, b) => Math.Sign(a.serveis.Count - b.serveis.Count)));
                    HashSet<string> prohibits = new HashSet<string>();
                    for (int i = 0; i < l_buckets_zona.Count; i++)
                    {
                        Bucket bpetit = l_buckets_zona[i];

                        if (bpetit.serveis.Count >= saturacio) continue;

                        List<Bucket> buckets_mes_grans = new List<Bucket>();
                        for (int j = i + 1; j < l_buckets_zona.Count; j++)
                            if (l_buckets_zona[j].serveis.Count > bpetit.serveis.Count)
                                buckets_mes_grans.Add(l_buckets_zona[j]);

                        List<Tuple<Servei, Bucket, double>> candidats = new List<Tuple<Servei, Bucket, double>>();
                        foreach (var b in buckets_mes_grans)
                        {
                            foreach (var s in b.serveis)
                            {
                                if (b.serveis.Count < saturacio) break;

                                // Controlar que el servei no es faci ja aquest dia
                                bool hi_es = false;
                                foreach (var bb in l_buckets_zona)
                                {
                                    if (bb == b) continue;
                                    if (bb.Dia.Value == bpetit.Dia.Value)
                                        foreach (var ss in bb.serveis)
                                            if (ss.Identificador == s.Identificador)
                                            {
                                                hi_es = true;
                                                break;
                                            }
                                    if (hi_es) break;
                                }
                                if (hi_es) continue;

                                if (prohibits.Contains(s.Identificador)) continue;

                                // Controlar que estigui dins del marge
                                var linia_matriu = matriu[s.Identificador];
                                bool puc_moure = false;
                                if (linia_matriu.ContainsKey(b.Dia.Value) && linia_matriu.ContainsKey(bpetit.Dia.Value))
                                {
                                    if (linia_matriu[b.Dia.Value] == 3 && linia_matriu[bpetit.Dia.Value] == 2 && Math.Abs((b.Dia.Value - bpetit.Dia.Value).TotalDays) <= s.Marge.TotalDays) puc_moure = true;
                                    else if (linia_matriu[b.Dia.Value] == 2 && linia_matriu[bpetit.Dia.Value] >= 2 && Math.Abs((b.Dia.Value - bpetit.Dia.Value).TotalDays) <= s.Marge.TotalDays) puc_moure = true;
                                }
                                if (!puc_moure) continue;

                                if (!MoureCorrecte(s, b.Dia.Value, bpetit.Dia.Value)) continue;

                                if (!bpetit.PucAssignarServei(s, true)) continue;

                                double min_dist = -1;
                                foreach (var spetit in bpetit.serveis)
                                {
                                    double dist = Context.DistanciaKm(spetit.Adreça.Y, spetit.Adreça.X, s.Adreça.Y, s.Adreça.X, true, false);
                                    if (min_dist == -1 || dist < min_dist)
                                        min_dist = dist;
                                }

                                if (min_dist >= max_dist) continue;

                                var dist_b_gros = b.DistanciaServei(s);

                                candidats.Add(new Tuple<Servei, Bucket, double>(s, b, min_dist - dist_b_gros));
                            }
                        }
                        candidats.Sort(new Comparison<Tuple<Servei, Bucket, double>>((a, b) => Math.Sign(a.Item3 - b.Item3)));

                        int falten = saturacio - bpetit.serveis.Count;
                        int fets = 0;
                        int index_candidat = 0;
                        while (index_candidat < candidats.Count && fets < falten)
                        {
                            var candidat = candidats[index_candidat];
                            var s = candidat.Item1;
                            bool hi_es = false;
                            foreach (var ss in bpetit.serveis)
                                if (ss.Identificador == s.Identificador)
                                {
                                    hi_es = true;
                                    break;
                                }
                            if (!hi_es)
                            {
                                if (bpetit.AssignarServei(s, buckets, true) == 0)
                                {
                                    candidat.Item2.serveis.Remove(s);
                                    prohibits.Add(s.Identificador);
                                    fets++;
                                    serevis_moguts++;
                                }
                            }
                            index_candidat++;
                        }
                    }
                }
            }
            return serevis_moguts;
        }

        private int RemoureServeisAllunyats()
        {
            int moguts = 0;
            int dist_proper = 10;

            Dictionary<string, List<Bucket>> dic_zones_buckets = new Dictionary<string, List<Bucket>>();
            foreach (var b in buckets) if (!dic_zones_buckets.ContainsKey(b.zona)) dic_zones_buckets.Add(b.zona, new List<Bucket>() { b }); else dic_zones_buckets[b.zona].Add(b);
            Dictionary<string, List<Bucket>> dic_cps = new Dictionary<string, List<Bucket>>();
            foreach (var b in buckets) foreach (var cp in b.cpAssignats) if (!dic_cps.ContainsKey(cp.Id)) dic_cps.Add(cp.Id, new List<Bucket>() { b }); else dic_cps[cp.Id].Add(b);

            foreach (var zona in dic_zones_buckets.Keys)
            {
                // Per cada zona, buscar serveis molt allunyats, i intentar ficar-los a buckets més propers
                var l_buckets_zona = dic_zones_buckets[zona];
                if (l_buckets_zona.Count > 1)
                {
                    var l = new List<Tuple<Servei, Bucket, double>>();
                    foreach (var b in l_buckets_zona)
                    {
                        foreach (var s in b.serveis)
                        {
                            double dist = b.DistanciaServei(s);
                            l.Add(new Tuple<Servei, Bucket, double>(s, b, dist));
                        }
                    }
                    l.Sort(new Comparison<Tuple<Servei, Bucket, double>>((a, b) => Math.Sign(b.Item3 - a.Item3)));
                    foreach (var tup in l)
                    {
                        if (tup.Item3 < dist_proper)
                            break;

                        // Tinc un servei que la distància amb el seu bucket és de més de 10km. 
                        // Busco un altre bucket que sigui més proper a ell
                        var s = tup.Item1;
                        var buc = tup.Item2;
                        var buckets_candidats = new List<Tuple<Bucket, double>>();
                        foreach (var b in l_buckets_zona)
                        {
                            if (b == buc) continue;
                            bool hi_es = false;
                            foreach (var bb in l_buckets_zona)
                            {
                                if (bb == b) continue;
                                if (bb.Dia.Value == buc.Dia.Value)
                                    foreach (var ss in bb.serveis)
                                        if (ss.Identificador == s.Identificador)
                                        {
                                            hi_es = true;
                                            break;
                                        }
                                if (hi_es) break;
                            }
                            if (hi_es) continue;
                            var linia_matriu = matriu[s.Identificador];
                            bool puc_moure = false;
                            if (linia_matriu[b.Dia.Value] == 3 && linia_matriu[buc.Dia.Value] == 2 && Math.Abs((b.Dia.Value - buc.Dia.Value).TotalDays) <= s.Marge.TotalDays) puc_moure = true;
                            else if (linia_matriu[b.Dia.Value] == 2 && linia_matriu[buc.Dia.Value] >= 2 && Math.Abs((b.Dia.Value - buc.Dia.Value).TotalDays) <= s.Marge.TotalDays) puc_moure = true;
                            if (!puc_moure) continue;
                            if (!buc.PucAssignarServei(s, true)) continue;

                            buckets_candidats.Add(new Tuple<Bucket, double>(b, b.DistanciaServei(s)));
                        }
                        buckets_candidats.Sort(new Comparison<Tuple<Bucket, double>>((a, b) => Math.Sign(a.Item2 - b.Item2)));
                        if (buckets_candidats.Count > 0 && buckets_candidats[0].Item2 < tup.Item3)
                        {
                            if (buckets_candidats[0].Item1.AssignarServei(s, buckets) == 0)
                            {
                                moguts++;
                                buc.serveis.Remove(s);
                            }
                        }
                    }
                }
            }
            return moguts;
        }
        #endregion

        #region Deprecated
        private int RepartirCarregaBucketsZonaOld()
        {
            int serevis_moguts = 0;
            Dictionary<string, List<Bucket>> dic_zones_buckets = new Dictionary<string, List<Bucket>>();
            foreach (var b in buckets) if (!dic_zones_buckets.ContainsKey(b.zona)) dic_zones_buckets.Add(b.zona, new List<Bucket>() { b }); else dic_zones_buckets[b.zona].Add(b);
            Dictionary<string, List<Bucket>> dic_cps = new Dictionary<string, List<Bucket>>();
            foreach (var b in buckets) foreach (var cp in b.cpAssignats) if (!dic_cps.ContainsKey(cp.Id)) dic_cps.Add(cp.Id, new List<Bucket>() { b }); else dic_cps[cp.Id].Add(b);

            foreach (var zona in dic_zones_buckets.Keys)
            {
                // Per cada zona, intentar repartir els buckets
                var l_buckets_zona = dic_zones_buckets[zona];
                l_buckets_zona.Sort(new Comparison<Bucket>((a, b) => Math.Sign(a.serveis.Count - b.serveis.Count)));
                if (l_buckets_zona.Count > 1)
                {
                    // Mirem si hi ha buckets molt més plens que altres
                    bool he_de_repartir = l_buckets_zona.First().serveis.Count < l_buckets_zona.Last().serveis.Count / 2;
                    HashSet<Servei> moguts = new HashSet<Servei>();
                    while (he_de_repartir)
                    {
                        Bucket bgros = l_buckets_zona.Last();
                        var serveis_bgros = new List<Tuple<Servei, Bucket, double>>();

                        // Intentar passar serveis del bucket més gros als altres
                        foreach (var sg in bgros.serveis)
                        {
                            foreach (var b_no_gros in l_buckets_zona)
                            {
                                bool trobat = false;
                                foreach (var cp in b_no_gros.cpAssignats) if (cp.Id == sg.CodiPostal) { trobat = true; break; }
                                if (!trobat) continue;

                                if (b_no_gros == bgros) continue;

                                bool hi_es = false;
                                foreach (var b in l_buckets_zona)
                                {
                                    if (b == bgros) continue;
                                    if (b.Dia == b_no_gros.Dia)
                                        foreach (var sp in b.serveis) if (sp.Identificador == sg.Identificador) { hi_es = true; break; }
                                }
                                if (hi_es) continue;
                                var linia_matriu = matriu[sg.Identificador];
                                bool puc_moure = false;

                                if (linia_matriu[bgros.Dia.Value] == 3 && linia_matriu[b_no_gros.Dia.Value] == 2 && Math.Abs((bgros.Dia.Value - b_no_gros.Dia.Value).TotalDays) <= sg.Marge.TotalDays) puc_moure = true;
                                else if (linia_matriu[bgros.Dia.Value] == 2 && linia_matriu[b_no_gros.Dia.Value] >= 2 && Math.Abs((bgros.Dia.Value - b_no_gros.Dia.Value).TotalDays) <= sg.Marge.TotalDays) puc_moure = true;

                                if (puc_moure)
                                {
                                    double dist_petit = b_no_gros.DistanciaServei(sg);
                                    double dist_gros = bgros.DistanciaServei(sg);

                                    // TODO: tenir en compte altres coses a més de la distància amb el bucket petit? la distància que tenia amb el bucket original, la diferència de dies amb el preferent...
                                    serveis_bgros.Add(new Tuple<Servei, Bucket, double>(sg, b_no_gros, b_no_gros.serveis.Count));
                                }
                            }
                        }
                        serveis_bgros.Sort(new Comparison<Tuple<Servei, Bucket, double>>((a, b) => Math.Sign(a.Item3 - b.Item3)));

                        bool algun = false;
                        while (serveis_bgros.Count > 0)
                        {
                            var tup = serveis_bgros.First();
                            var s = tup.Item1;
                            var bpetit = tup.Item2;
                            if (!moguts.Contains(s) && bpetit.AssignarServei(s, buckets) == 0)
                            {
                                moguts.Add(s);
                                serevis_moguts++;
                                bgros.serveis.Remove(s);
                                algun = true;
                                break;
                            }
                            serveis_bgros.RemoveAt(0);
                        }
                        if (!algun) break;  // TODO: intentar repartir amb el segon bucket més petit?

                        l_buckets_zona.Sort(new Comparison<Bucket>((a, b) => Math.Sign(a.serveis.Count - b.serveis.Count)));
                        he_de_repartir = l_buckets_zona.First().serveis.Count < l_buckets_zona.Last().serveis.Count / 2;
                    }
                }
            }
            return serevis_moguts;
        }

        private int RepartirCarregaBucketsOld()
        {
            int serevis_moguts = 0;
            Dictionary<string, List<Bucket>> dic_zones_buckets = new Dictionary<string, List<Bucket>>();
            foreach (var b in buckets) if (!dic_zones_buckets.ContainsKey(b.zona)) dic_zones_buckets.Add(b.zona, new List<Bucket>() { b }); else dic_zones_buckets[b.zona].Add(b);

            foreach (var zona in dic_zones_buckets.Keys)
            {
                // Per cada zona, intentar repartir els buckets
                var l_buckets_zona = dic_zones_buckets[zona];
                l_buckets_zona.Sort(new Comparison<Bucket>((a, b) => Math.Sign(a.serveis.Count - b.serveis.Count)));
                if (l_buckets_zona.Count > 1)
                {
                    // Mirem si hi ha buckets molt més plens que altres
                    bool he_de_repartir = l_buckets_zona.First().serveis.Count < l_buckets_zona.Last().serveis.Count / 2;
                    while (he_de_repartir)
                    {
                        Bucket bgros = l_buckets_zona.Last();
                        Bucket bpetit = l_buckets_zona.First();
                        List<Tuple<Servei, double>> serveis_bgros = new List<Tuple<Servei, double>>();

                        // Intentar passar serveis del bucket més gros al més petit
                        foreach (var sg in bgros.serveis)
                        {
                            bool hi_es = false;
                            foreach (var b in l_buckets_zona)
                            {
                                if (b == bgros) continue;
                                if (b.Dia == bpetit.Dia)
                                    foreach (var sp in b.serveis) if (sp.Identificador == sg.Identificador) { hi_es = true; break; }
                            }
                            if (hi_es) continue;
                            var linia_matriu = matriu[sg.Identificador];
                            bool puc_moure = false;

                            if (linia_matriu[bgros.Dia.Value] == 3 && linia_matriu[bpetit.Dia.Value] == 2 && Math.Abs((bgros.Dia.Value - bpetit.Dia.Value).TotalDays) <= sg.Marge.TotalDays) puc_moure = true;
                            else if (linia_matriu[bgros.Dia.Value] == 2 && linia_matriu[bpetit.Dia.Value] >= 2 && Math.Abs((bgros.Dia.Value - bpetit.Dia.Value).TotalDays) <= 1) puc_moure = true;

                            if (puc_moure)
                            {
                                double dist_petit = bpetit.DistanciaServei(sg);
                                double dist_gros = bgros.DistanciaServei(sg);

                                // TODO: tenir en compte altres coses a més de la distància amb el bucket petit? la distància que tenia amb el bucket original, la diferència de dies amb el preferent...
                                serveis_bgros.Add(new Tuple<Servei, double>(sg, dist_petit));
                            }
                        }
                        serveis_bgros.Sort(new Comparison<Tuple<Servei, double>>((a, b) => Math.Sign(a.Item2 - b.Item2)));

                        bool algun = false;
                        while (serveis_bgros.Count > 0)
                        {
                            var s = serveis_bgros.First().Item1;
                            if (bpetit.AssignarServei(s, buckets) == 0)
                            {
                                serevis_moguts++;
                                bgros.serveis.Remove(s);
                                algun = true;
                                break;
                            }
                            serveis_bgros.RemoveAt(0);
                        }
                        if (!algun) break;  // TODO: intentar repartir amb el segon bucket més petit?

                        l_buckets_zona.Sort(new Comparison<Bucket>((a, b) => Math.Sign(a.serveis.Count - b.serveis.Count)));
                        he_de_repartir = l_buckets_zona.First().serveis.Count < l_buckets_zona.Last().serveis.Count / 2;
                    }
                }
            }
            return serevis_moguts;
        }
        #endregion

        private void RepetirBuckets(bool tenir_en_compte_data_ultima_visita, DateTime data_fi)
        {
            foreach (var b in buckets)
            {
                b.CarregaTotalAbans = b.CarregaTotal();
            }

            int n_repeticions;
            int setmanes_planificar = (int)Math.Ceiling((data_fi - ConfigClient.DataInici).TotalDays / 7.0);
            int setmanes_classificacio = 2;
            n_repeticions = (int)Math.Ceiling((double)setmanes_planificar / setmanes_classificacio);
            List<Bucket> buckets_repetits = new List<Bucket>();
            for (int i = 1; i < n_repeticions; i++)
            {
                var dic_bucketsb = new Dictionary<Bucket, Bucket>();
                foreach (var b in buckets)
                {
                    Bucket b2 = b.FerCopia();
                    b2.Dia = b2.Dia.Value.AddDays(14 * i);
                    b2.serveis = new List<Servei>();
                    b2.bucketB = null;
                    if (b.bucketB != null)
                        dic_bucketsb.Add(b.bucketB, b2);
                    buckets_repetits.Add(b2);
                    if (!buckets_dies.ContainsKey(b2.Dia.Value.ToShortDateString())) buckets_dies.Add(b2.Dia.Value.ToShortDateString(), new HashSet<Bucket>());
                    buckets_dies[b2.Dia.Value.ToShortDateString()].Add(b2);
                    foreach (var s in serveis_buckets.Keys)
                        if (serveis_buckets[s].Contains(b))
                            serveis_buckets[s].Add(b2);
                    if (dic_bucketsb.ContainsKey(b))
                    {
                        dic_bucketsb[b].bucketB = b2;
                        b2.bucketB = dic_bucketsb[b];
                    }
                }
            }
            buckets.AddRange(buckets_repetits);

            //if (tenir_en_compte_data_ultima_visita) return;

            bool EliminarDobleMensual = false;

            if (EliminarDobleMensual)
            {
                // Eliminar doble mensual
                int neliminats = 0, nmensuals = 0;
                HashSet<string> cps_tractats = new HashSet<string>();
                foreach (var b in buckets)
                    for (int i = 0; i < b.cpAssignats.Count; i++)
                    {
                        InfoCP cp = b.cpAssignats[i];

                        if (cp.Id == "43800")
                            cp.ToString();

                        if (!cp.Mensual) continue;
                        if (cps_tractats.Contains(cp.Id)) continue;
                        nmensuals++;

                        // Creo la llista de tots els buckets que tenen aquest cp mensual
                        List<Tuple<Bucket, DateTime>> l = new List<Tuple<Bucket, DateTime>>();
                        l.Add(new Tuple<Bucket, DateTime>(b, b.Dia.Value));
                        foreach (var b2 in buckets)
                        {
                            if (b2 == b) continue;

                            for (int j = 0; j < b2.cpAssignats.Count; j++)
                            {
                                var cp2 = b2.cpAssignats[j];
                                if (cp2.Id != cp.Id) continue;

                                l.Add(new Tuple<Bucket, DateTime>(b2, b2.Dia.Value));
                                break;
                            }
                        }
                        l.Sort(new Comparison<Tuple<Bucket, DateTime>>((a1, b1) => Math.Sign((a1.Item2 - b1.Item2).TotalDays)));
                        if (l.Count < 2) continue;

                        //if (l[0].Item1.CarregaTotal() > l[1].Item1.CarregaTotal() * 1.3)
                        //    continue;
                        //if (l[1].Item1.CarregaTotal() > l[0].Item1.CarregaTotal() * 1.3)
                        //    continue;

                        // Decideixo si eliminar els parells o els senars
                        int eliminat_ij = 0;
                        if (l[0].Item1.cpAssignats.Count == l[1].Item1.cpAssignats.Count)
                        {
                            // A igualtat de condicions, aleatòriament
                            var rand = r.Next();
                            if (rand < 0.5) eliminat_ij = 1;
                            else eliminat_ij = 2;
                        }
                        else
                        {
                            // Eliminar el que tingui cps més llunyans en mitjana
                            double mitjana_dist_0 = 0, mitjana_dist_1 = 0;
                            int n_dist_0 = 0, n_dist_1 = 0;
                            foreach (var cpp in l[0].Item1.cpAssignats)
                            {
                                //var dist = Context.DistanciaKm(cpp.YCentroide, cpp.XCentroide, cp.YCentroide, cp.XCentroide);
                                var dist = Context.DistanciaKm(cpp, cp);
                                mitjana_dist_0 += dist;
                                n_dist_0++;
                            }
                            foreach (var cpp in l[1].Item1.cpAssignats)
                            {
                                //var dist = Context.DistanciaKm(cpp.YCentroide, cpp.XCentroide, cp.YCentroide, cp.XCentroide);
                                var dist = Context.DistanciaKm(cpp, cp);
                                mitjana_dist_1 += dist;
                                n_dist_1++;
                            }
                            mitjana_dist_0 /= n_dist_0;
                            mitjana_dist_1 /= n_dist_1;
                            if (mitjana_dist_0 < mitjana_dist_1) eliminat_ij = 2;
                            else eliminat_ij = 1;
                            if (n_dist_0 == 0) eliminat_ij = 2;
                            else if (n_dist_1 == 0) eliminat_ij = 1;
                        }
                        int index_eliminar = 0, index_ampliar = 1;
                        if (eliminat_ij == 2) { index_eliminar = 1; index_ampliar = 0; }


                        bool contevalls = false;
                        //for (int ii = 0; ii < l[index_ampliar].Item1.cpAssignats.Count; ii++)
                        //    if (l[index_ampliar].Item1.cpAssignats[ii].Id=="43800")
                        //        contevalls = true;

                        if (cp.Id == "43800")
                            contevalls = true;

                        if (!contevalls && l[index_ampliar].Item1.CarregaTotal() > l[index_eliminar].Item1.CarregaTotal() * 1.3)
                            continue;

                        
                        // Elimino els cps dels buckets que he decidit eliminar i amplio els altres
                        while (index_eliminar < l.Count && index_ampliar < l.Count)
                        {
                            var b_eliminar = l[index_eliminar].Item1;
                            var b_ampliar = l[index_ampliar].Item1;
                            for (int ii = 0; ii < b_eliminar.cpAssignats.Count; ii++)
                                if (b_eliminar.cpAssignats[ii].Id == cp.Id)
                                {
                                    b_eliminar.cpAssignats.RemoveAt(ii);
                                    b_eliminar.carregues.RemoveAt(ii);
                                    b_eliminar.RecalculaCarregaTotal();
                                    neliminats++;
                                    break;
                                }
                            for (int ii = 0; ii < b_ampliar.cpAssignats.Count; ii++)
                                if (b_ampliar.cpAssignats[ii].Id == cp.Id)
                                {
                                    b_ampliar.carregues[ii] *= 2;
                                    b_ampliar.RecalculaCarregaTotal();
                                    break;
                                }
                            index_eliminar += 2;
                            index_ampliar += 2;
                        }

                        cps_tractats.Add(cp.Id);
                    }
            }
        }

        private void EliminarBuckets(DateTime inici, DateTime fi)
        {
            for (int i = 0; i < buckets.Count; i++)
            {
                var b = buckets[i];
                if (b.Dia < inici || b.Dia > fi)
                {
                    foreach (var d in buckets_dies.Keys)
                        if (buckets_dies[d].Contains(b))
                            buckets_dies[d].Remove(b);
                    foreach (var s in serveis_buckets.Keys)
                        if (serveis_buckets[s].Contains(b))
                            serveis_buckets[s].Remove(b);
                    buckets.RemoveAt(i--);
                }
            }
            foreach (var b in buckets)
            {
                if (!buckets.Contains(b.bucketB))
                    b.bucketB = null;
            }
        }

        private List<Servei> OrdenarPerPreferencies(List<Servei> llista_serveis)
        {
            List<Tuple<Servei, int>> l = new List<Tuple<Servei, int>>();
            foreach (var s in llista_serveis)
            {
                int n = 0;
                for (int i = 0; i < s.Preferencies.Length; i++)
                {
                    if (s.Preferencies[i] == 'V')
                    {
                        n += 1000;
                    }
                }

                if (s.Frequencia.TotalDays == 2) n += 100;
                if (s.Frequencia.TotalDays == 5) n += 75;
                if (s.Frequencia.TotalDays == 10) n += 50;
                if (s.Frequencia.TotalDays == 15) n += 25;
                if (s.Frequencia.TotalDays == 20) n += 20;
                //if (s.Frequencia.TotalDays>20) n += 25;

                if (s.Frequencia.TotalDays == 2) n += 20;
                if (s.Frequencia.TotalDays == 5) n += 25;
                if (s.Frequencia.TotalDays == 10) n += 50;
                if (s.Frequencia.TotalDays == 15) n += 75;
                if (s.Frequencia.TotalDays == 20) n += 100;
                if (s.Frequencia.TotalDays>20) n += 200;

                l.Add(new Tuple<Servei, int>(s, n));
            }
            l.Sort(new Comparison<Tuple<Servei, int>>((a, b) => Math.Sign(a.Item2 - b.Item2)));
            List<Servei> l2 = new List<Servei>();
            foreach (var tup in l)
                l2.Add(tup.Item1);
            return l2;
        }

        private int CalcularNBS(List<Tuple<Bucket, DateTime>> bu, Servei s, int freq_buck, DateTime dataInicial)
        {
            DateTime data_inici = dataInicial;
            DateTime data_fi = dataInicial;

            if (s.Frequencia.TotalDays == 5)
                data_fi = data_inici.AddDays(7);
            else
            if (s.Frequencia.TotalDays == 10)
                data_fi = data_inici.AddDays(14);
            else
            if (s.Frequencia.TotalDays == 20)
                data_fi = data_inici.AddDays(30);
            else
            if (s.Frequencia.TotalDays == 40)
                data_fi = data_inici.AddDays(60);
            else
                return (int)Math.Round((double)(s.Frequencia.TotalDays * 7 / 5) / freq_buck);

            int contador = 0;
            for (int i = 0; i<bu.Count; i++)
            {
                if (bu[i].Item2 < data_fi)
                    contador++;
            }

            return contador;

            //int nbs = (int)Math.Round((double)(s.Frequencia.TotalDays * 7 / 5) / freq_buck);

            //return (int)Math.Round((double)(s.Frequencia.TotalDays * 7 / 5) / freq_buck);
        }

        private DateTime CalcularUltimaVisita(Servei s, bool tenir_en_compte_data_ultima_visita, DateTime data_inici, DateTime data_fi)
        {
            bool AfegirTotsElsBuckets = true;
            DateTime dataInicialCalculada = DateTime.Now;

            if (tenir_en_compte_data_ultima_visita)
            {
                DateTime ultimaVisita = s.UltimaVisita;
                int ddies = (int)s.Frequencia.TotalDays * 7 / 5;
                DateTime proximaVisita = ultimaVisita.AddDays(ddies);
                var Matrium = matriu[s.Identificador];
                int m = (int)s.Marge.TotalDays;
                if (m == 0) m = 1;
                //dataInicialCalculada = proximaVisita.AddDays(-m);
                dataInicialCalculada = s.UltimaVisita;
                if (s.Frequencia.TotalDays <= 5) dataInicialCalculada = dataInicialCalculada.AddDays(2);
                else if (s.Frequencia.TotalDays > 5)
                {
                    dataInicialCalculada = dataInicialCalculada.AddDays(Context.Marge(s.Frequencia));
                    if (s.Frequencia.TotalDays >= 40)
                    {
                        if (proximaVisita.AddDays(-m) > dataInicialCalculada)
                            dataInicialCalculada = proximaVisita.AddDays(-m);
                    }
                }
                if (dataInicialCalculada > ConfigClient.DataInici)
                    AfegirTotsElsBuckets = false;
            }

            var lbuckscp = new List<Tuple<Bucket, DateTime>>();
            var lbuckscpCopia = new List<Tuple<Bucket, DateTime>>();
            int freq_buck = (int)Math.Ceiling((data_fi - data_inici).TotalDays / serveis_buckets[s.Identificador].Count);
            DateTime d = data_inici;
            if (serveis_buckets[s.Identificador].Count > 0)
            {
                foreach (var b in serveis_buckets[s.Identificador])
                {
                    if (AfegirTotsElsBuckets)
                    {
                        lbuckscp.Add(new Tuple<Bucket, DateTime>(b, b.Dia.Value));
                    }
                    else
                    {
                        lbuckscpCopia.Add(new Tuple<Bucket, DateTime>(b, b.Dia.Value));
                        if (b.Dia >= dataInicialCalculada)
                            lbuckscp.Add(new Tuple<Bucket, DateTime>(b, b.Dia.Value));
                    }
                }

                lbuckscp.Sort(new Comparison<Tuple<Bucket, DateTime>>((a, b) => Math.Sign((a.Item2 - b.Item2).TotalDays)));
                var diad = Context.PassaDies(s.UltimaVisita.AddDays(s.Frequencia.TotalDays * 7 / 5), Context.Marge(s.Frequencia));
                if (s.Frequencia.TotalDays * 7 / 5 <= freq_buck || diad < ConfigClient.DataInici)
                {
                    // No hi ha varis buckets per escollir
                    d = lbuckscp[0].Item2.AddDays(-(int)s.Frequencia.TotalDays * 7 / 5);
                }
                else
                {
                    // Hi ha varis buckets per escollir
                    int nbs = 0;
                    if (AfegirTotsElsBuckets)
                        nbs = CalcularNBS(lbuckscp, s, freq_buck, ConfigClient.DataInici);
                    else
                        nbs = CalcularNBS(lbuckscp, s, freq_buck, dataInicialCalculada);

                    if (lbuckscp.Count < nbs) nbs = lbuckscp.Count;

                    // Si no hi ha cap serveis asignat del CP tria un bucket que no estigui ple
                    List<int> idxNoPlens = new List<int>();
                    List<int> idxCorrectes = new List<int>();

                    for (int yy = 0; yy < nbs; yy++)
                    {
                        int visitesVehicle = 20;
                        if (lbuckscp[yy].Item1.cpAssignats.Count > 0)
                            visitesVehicle = lbuckscp[yy].Item1.cpAssignats[0].CAPACITAT_VEHICLE;
                        double CarregaPrevista = lbuckscp[yy].Item1.CarregaTotalAbans * visitesVehicle;
                        int NumServeisPrevistos = (int)Math.Ceiling(CarregaPrevista);

                        // TODO: Comprovar alguna restricció més
                        bool ok1 = (s.Preferencies[0] == 'V') && (lbuckscp[yy].Item1.Dia.Value.DayOfWeek == DayOfWeek.Monday);
                        bool ok2 = (s.Preferencies[1] == 'V') && (lbuckscp[yy].Item1.Dia.Value.DayOfWeek == DayOfWeek.Tuesday);
                        bool ok3 = (s.Preferencies[2] == 'V') && (lbuckscp[yy].Item1.Dia.Value.DayOfWeek == DayOfWeek.Wednesday);
                        bool ok4 = (s.Preferencies[3] == 'V') && (lbuckscp[yy].Item1.Dia.Value.DayOfWeek == DayOfWeek.Thursday);
                        bool ok5 = (s.Preferencies[4] == 'V') && (lbuckscp[yy].Item1.Dia.Value.DayOfWeek == DayOfWeek.Friday);
                        bool diaCorrecte = (ok1 || ok2 || ok3 || ok4 || ok5);

                        if (diaCorrecte) idxCorrectes.Add(yy);
                        if (lbuckscp[yy].Item1.serveis.Count < (NumServeisPrevistos * 0.9) && diaCorrecte)
                            idxNoPlens.Add(yy);
                    }


                    List<int> llista = new List<int>();

                    if (idxNoPlens.Count > 0) llista = idxNoPlens;
                    else if (idxCorrectes.Count > 0) llista = idxCorrectes;

                    int index = -1;
                    if (llista.Count > 0)
                    {
                        // triar el dia per proximitat
                        int idxBucketMin = -1;
                        double distMin = 1000000;
                        int nums = 0;
                        for (int yyy = 0; yyy < llista.Count; yyy++)
                        {
                            double distancia = lbuckscp[yyy].Item1.DistanciaServei(s);

                            if (distMin > (distancia * 0.7))
                            {
                                // actualitzar només si hi ha molta diferencia
                                // sino, tenir en compte com de plens estan els buckets
                                if ((distMin * 0.6) > distancia)
                                {
                                    distMin = distancia;
                                    idxBucketMin = llista[yyy];
                                    nums = yyy;
                                }
                                else
                                {
                                    if (lbuckscp[yyy].Item1.serveis.Count < lbuckscp[nums].Item1.serveis.Count)
                                    {
                                        distMin = distancia;
                                        idxBucketMin = llista[yyy];
                                        nums = yyy;
                                    }
                                }
                            }
                        }

                        if (idxBucketMin != -1)
                            index = idxBucketMin;
                    }
                    else
                    {
                        index = r.Next(nbs);
                    }

                    if (lbuckscp.Count == 0)
                        d = s.UltimaVisita;
                    else 
                        d = lbuckscp[index].Item2.AddDays(-(int)s.Frequencia.TotalDays * 7 / 5);
                }
            }

            if (s.Frequencia.TotalDays <= 5) d = Context.AnteriorDiaVisitable(s, d);
            bool reassignar_ultima_visita = s.Frequencia.TotalDays >= 5 && (d - s.UltimaVisita).TotalDays > Context.Marge(s.Frequencia);
            //reassignar_ultima_visita |= (d - s.UltimaVisita).TotalDays >= s.Frequencia.TotalDays * 7 / 5 * 1.3;
            if (tenir_en_compte_data_ultima_visita && reassignar_ultima_visita)
            {
                //if (s.Frequencia.TotalDays != 10)
                    d = s.UltimaVisita;
            }
            return d;
        }

        private Dictionary<string, int> AssignarServeisBuckets2(bool tenir_en_compte_data_ultima_visita, DateTime data_inici, DateTime data_fi)
        {
            Dictionary<string, int> assignats = new Dictionary<string, int>();

            llista_serveis = OrdenarPerPreferencies(llista_serveis);

            for (int iis = 0; iis < llista_serveis.Count; iis++)
            {
                var s = llista_serveis[iis];
                //if (s.Frequencia.TotalDays > 20) continue;
                // Només bisetmanals, setmanals, quinzenals i mensuals

                string zona_servei = Context.AssignarZona(s);
                List<Tuple<DateTime, int>> dies;
                //bool visita_adicional = false;

                DateTime d = CalcularUltimaVisita(s, tenir_en_compte_data_ultima_visita, data_inici, data_fi);
                d = s.UltimaVisita;

                Context.PlanificarVisitesServeiComplet(s, d, data_inici, data_fi, out dies);

                Dictionary<int, int> ns = new Dictionary<int, int>();
                foreach (var t in dies) if (!ns.ContainsKey(t.Item2)) ns.Add(t.Item2, 1); else ns[t.Item2]++;
                if (ns.ContainsKey(1) && !ns.ContainsKey(0) && !ns.ContainsKey(2) && !ns.ContainsKey(3) && !ns.ContainsKey(4))
                    continue;

                if (ns.ContainsKey(4) && !ns.ContainsKey(0) && !ns.ContainsKey(2) && !ns.ContainsKey(3))
                {
                    // Servei obligatori
                    #region Obligatoris
                    List<DateTime> dies_visita = new List<DateTime>();
                    foreach (var t in dies) if (t.Item2 == 4) dies_visita.Add(t.Item1);
                    //n2 += dies_visita.Count;
                    foreach (var dia in dies_visita)
                    {
                        // Per cada dia que s'hagi de planificar aquest servei
                        bool assignat = false;

                        // Busco dels buckets del seu CP i dia, el més proper
                        List<Tuple<Bucket, double>> buckets_dia = new List<Tuple<Bucket, double>>();
                        foreach (var b in serveis_buckets[s.Identificador])
                            if (b.Dia.Value == dia)
                                buckets_dia.Add(new Tuple<Bucket, double>(b, b.DistanciaServei(s)));
                        buckets_dia.Sort(new Comparison<Tuple<Bucket, double>>((a, b) => Math.Sign(a.Item2 - b.Item2)));
                        foreach (var tup in buckets_dia)
                            if (tup.Item1.AssignarServei(s, buckets) == 0)
                            {
                                assignat = true;
                                string tipus = "OB_CP_OK"; if (!assignats.ContainsKey(tipus)) assignats.Add(tipus, 1); else assignats[tipus]++;
                                break;
                            }

                        // Si no puc assignar-lo a les bones, l'assigno a les males
                        if (!assignat)
                            if (buckets_dia.Count > 0)
                                if (buckets_dia[0].Item1.AssignarServei(s, buckets, true) == 0)
                                {
                                    assignat = true;
                                    string tipus = "OB_CP_Forsant"; if (!assignats.ContainsKey(tipus)) assignats.Add(tipus, 1); else assignats[tipus]++;
                                }

                        // Si no puc ni a les males, busco el bucket més proper del seu dia
                        if (!assignat)
                        {
                            buckets_dia = new List<Tuple<Bucket, double>>();
                            foreach (var b in buckets)
                                if (b.Dia.Value == dia)
                                    buckets_dia.Add(new Tuple<Bucket, double>(b, b.DistanciaServei(s)));
                            buckets_dia.Sort(new Comparison<Tuple<Bucket, double>>((a, b) => Math.Sign(a.Item2 - b.Item2)));
                            foreach (var tup in buckets_dia)
                                if (tup.Item1.AssignarServei(s, buckets) == 0)
                                {
                                    assignat = true;
                                    string tipus = "OB_NOCP"; if (!assignats.ContainsKey(tipus)) assignats.Add(tipus, 1); else assignats[tipus]++;
                                    break;
                                }
                        }

                        if (!assignat)
                        {
                            string tipus = "OB_KO"; if (!assignats.ContainsKey(tipus)) assignats.Add(tipus, 1); else assignats[tipus]++;
                        }
                    }
                    #endregion
                }
                else if (ns.ContainsKey(2) || ns.ContainsKey(3))
                {
                    // Servei no obligatori
                    #region No Obligatoris
                    List<DateTime> dies_visita = new List<DateTime>(), dies2 = new List<DateTime>();
                    foreach (var t in dies)
                        if (t.Item2 >= 3)
                        {
                            if (!dies_visita.Contains(t.Item1))
                                dies_visita.Add(t.Item1);
                        }
                        else if (t.Item2 == 2) dies2.Add(t.Item1);

                    for (int idia = 0; idia < dies_visita.Count; idia++)
                    {
                        var dia = dies_visita[idia];
                        bool assignat = false;

                        // Puntuo dies, segons preferència i proximitat
                        List<Tuple<Bucket, double>> buckets_dia = new List<Tuple<Bucket, double>>();
                        foreach (var b in serveis_buckets[s.Identificador])
                            if (b.Dia.Value == dia)
                            {
                                double puntuacio = 1000.0 - b.DistanciaServei(s);
                                buckets_dia.Add(new Tuple<Bucket, double>(b, puntuacio));
                            }
                        int marge = 1;
                        while (true)
                        {
                            foreach (var b in serveis_buckets[s.Identificador])
                            {
                                var d2 = SumaDiesLaborals(dia, marge);
                                var d3 = SumaDiesLaborals(dia, -marge);
                                if ((b.Dia.Value == d2 && dies2.Contains(d2)) || (b.Dia.Value == d3 && dies2.Contains(d3)))
                                {
                                    bool hies = false;
                                    foreach (var s1 in b.serveis)
                                        if (s1.Identificador == s.Identificador) { hies = true; break; }
                                    if (hies) continue;
                                    foreach (var bb in buckets)
                                    {
                                        if (bb.Dia.Value == b.Dia.Value && b.zona == bb.zona && bb != b)
                                            foreach (var s1 in bb.serveis) if (s1.Identificador == s.Identificador) { hies = true; break; }
                                        if (hies) break;
                                    }
                                    if (hies) continue;
                                    double puntuacio = 1000.0 - marge * 100 - b.DistanciaServei(s);
                                    buckets_dia.Add(new Tuple<Bucket, double>(b, puntuacio));
                                }
                            }
                            marge++;
                            var d22 = SumaDiesLaborals(dia, marge);
                            var d33 = SumaDiesLaborals(dia, -marge);
                            if (!dies2.Contains(d22) && !dies2.Contains(d33)) break;
                            if (marge > s.Marge.TotalDays) break;
                        }

                        buckets_dia.Sort(new Comparison<Tuple<Bucket, double>>((a, b) => Math.Sign(b.Item2 - a.Item2)));
                        foreach (var tup in buckets_dia)
                            if (tup.Item1.AssignarServei(s, buckets) == 0)
                            {
                                var dif = (tup.Item1.Dia.Value - dia).TotalDays;
                                for (int jdia = idia + 1; jdia < dies_visita.Count; jdia++) dies_visita[jdia] = dies_visita[jdia].AddDays((int)dif);
                                assignat = true;
                                string tipus = "NOOB_CP"; if (!assignats.ContainsKey(tipus)) assignats.Add(tipus, 1); else assignats[tipus]++;
                                break;
                            }

                        if (!assignat)
                        {
                            foreach (var tup in buckets_dia)
                                if (tup.Item1.AssignarServei(s, buckets, true) == 0)
                                {
                                    var dif = (tup.Item1.Dia.Value - dia).TotalDays;
                                    for (int jdia = idia + 1; jdia < dies_visita.Count; jdia++) dies_visita[jdia] = dies_visita[jdia].AddDays((int)dif);
                                    assignat = true;
                                    string tipus = "NOOB_CP_Forçant"; if (!assignats.ContainsKey(tipus)) assignats.Add(tipus, 1); else assignats[tipus]++;
                                    break;
                                }
                        }

                        // Si no he pogut assignar-lo, provo amb els buckets de la seva zona
                        if (!assignat)
                        {
                            buckets_dia = new List<Tuple<Bucket, double>>();
                            foreach (var b in buckets)
                            {
                                if (b.zona != zona_servei) continue;
                                if (b.Dia.Value == dia)
                                {
                                    double puntuacio = 1000.0 - b.DistanciaServei(s);
                                    buckets_dia.Add(new Tuple<Bucket, double>(b, puntuacio));
                                }
                            }
                            marge = 1;
                            while (true)
                            {
                                foreach (var b in buckets)
                                {
                                    if (b.zona != zona_servei) continue;
                                    var d2 = SumaDiesLaborals(dia, marge);
                                    var d3 = SumaDiesLaborals(dia, -marge);
                                    if ((b.Dia.Value == d2 && dies2.Contains(d2)) || (b.Dia.Value == d3 && dies2.Contains(d3)))
                                    {
                                        bool hies = false;
                                        foreach (var s1 in b.serveis)
                                            if (s1.Identificador == s.Identificador) { hies = true; break; }
                                        if (hies) continue;
                                        foreach (var bb in buckets)
                                        {
                                            if (bb.Dia.Value == b.Dia.Value && b.zona == bb.zona && bb != b)
                                                foreach (var s1 in bb.serveis) if (s1.Identificador == s.Identificador) { hies = true; break; }
                                            if (hies) break;
                                        }
                                        if (hies) continue;
                                        double puntuacio = 1000.0 - marge * 100 - b.DistanciaServei(s);
                                        buckets_dia.Add(new Tuple<Bucket, double>(b, puntuacio));
                                    }
                                }
                                marge++;
                                var d22 = SumaDiesLaborals(dia, marge);
                                var d33 = SumaDiesLaborals(dia, -marge);
                                if (!dies2.Contains(d22) && !dies2.Contains(d33)) break;
                                if (marge > s.Marge.TotalDays) break;
                            }

                            buckets_dia.Sort(new Comparison<Tuple<Bucket, double>>((a, b) => Math.Sign(b.Item2 - a.Item2)));
                            foreach (var tup in buckets_dia)
                                if (tup.Item1.AssignarServei(s, buckets) == 0)
                                {
                                    var dif = (tup.Item1.Dia.Value - dia).TotalDays;
                                    for (int jdia = idia + 1; jdia < dies_visita.Count; jdia++) dies_visita[jdia] = dies_visita[jdia].AddDays((int)dif);
                                    assignat = true;
                                    string tipus = "NOOB_Zona";
                                    if (!assignats.ContainsKey(tipus)) assignats.Add(tipus, 1); else assignats[tipus]++;
                                    break;
                                }

                            if (!assignat)
                            {
                                foreach (var tup in buckets_dia)
                                    if (tup.Item1.AssignarServei(s, buckets, true) == 0)
                                    {
                                        var dif = (tup.Item1.Dia.Value - dia).TotalDays;
                                        for (int jdia = idia + 1; jdia < dies_visita.Count; jdia++) dies_visita[jdia] = dies_visita[jdia].AddDays((int)dif);
                                        assignat = true;
                                        string tipus = "NOOB_Zona_Forçant";
                                        if (!assignats.ContainsKey(tipus)) assignats.Add(tipus, 1); else assignats[tipus]++;
                                        break;
                                    }
                            }
                        }

                        // Si no he pogut assignar-lo, provo amb els buckets fora zona
                        if (!assignat)
                        {
                            buckets_dia = new List<Tuple<Bucket, double>>();
                            foreach (var b in buckets)
                            {
                                if (b.zona == zona_servei) continue;
                                if (b.Dia.Value == dia)
                                {
                                    double puntuacio = 1000.0 - b.DistanciaServei(s);
                                    buckets_dia.Add(new Tuple<Bucket, double>(b, puntuacio));
                                }
                            }
                            marge = 1;
                            while (true)
                            {
                                foreach (var b in buckets)
                                {
                                    if (b.zona == zona_servei) continue;
                                    var d2 = SumaDiesLaborals(dia, marge);
                                    var d3 = SumaDiesLaborals(dia, -marge);
                                    if ((b.Dia.Value == d2 && dies2.Contains(d2)) || (b.Dia.Value == d3 && dies2.Contains(d3)))
                                    {
                                        bool hies = false;
                                        foreach (var s1 in b.serveis)
                                            if (s1.Identificador == s.Identificador) { hies = true; break; }
                                        if (hies) continue;
                                        foreach (var bb in buckets)
                                        {
                                            if (bb.Dia.Value == b.Dia.Value && b.zona == bb.zona && bb != b)
                                                foreach (var s1 in bb.serveis) if (s1.Identificador == s.Identificador) { hies = true; break; }
                                            if (hies) break;
                                        }
                                        if (hies) continue;
                                        double puntuacio = 1000.0 - marge * 100 - b.DistanciaServei(s);
                                        buckets_dia.Add(new Tuple<Bucket, double>(b, puntuacio));
                                    }
                                }
                                marge++;
                                var d22 = SumaDiesLaborals(dia, marge);
                                var d33 = SumaDiesLaborals(dia, -marge);
                                if (!dies2.Contains(d22) && !dies2.Contains(d33)) break;
                                if (marge > s.Marge.TotalDays) break;
                            }

                            buckets_dia.Sort(new Comparison<Tuple<Bucket, double>>((a, b) => Math.Sign(b.Item2 - a.Item2)));
                            foreach (var tup in buckets_dia)
                                if (tup.Item1.AssignarServei(s, buckets) == 0)
                                {
                                    var dif = (tup.Item1.Dia.Value - dia).TotalDays;
                                    for (int jdia = idia + 1; jdia < dies_visita.Count; jdia++) dies_visita[jdia] = dies_visita[jdia].AddDays((int)dif);
                                    assignat = true;
                                    string tipus = "NOOB_ForaZona";
                                    if (!assignats.ContainsKey(tipus)) assignats.Add(tipus, 1); else assignats[tipus]++;
                                    break;
                                }

                            if (!assignat)
                            {
                                foreach (var tup in buckets_dia)
                                    if (tup.Item1.AssignarServei(s, buckets, true) == 0)
                                    {
                                        var dif = (tup.Item1.Dia.Value - dia).TotalDays;
                                        for (int jdia = idia + 1; jdia < dies_visita.Count; jdia++) dies_visita[jdia] = dies_visita[jdia].AddDays((int)dif);
                                        assignat = true;
                                        string tipus = "NOOB_ForaZona_Forçant";
                                        if (!assignats.ContainsKey(tipus)) assignats.Add(tipus, 1); else assignats[tipus]++;
                                        break;
                                    }
                            }
                        }

                        if (!assignat)
                        {
                            // No hi ha maneres
                            string tipus = "NOOB_KO"; if (!assignats.ContainsKey(tipus)) assignats.Add(tipus, 1); else assignats[tipus]++;
                        }
                    }
                    #endregion
                }
                else
                    MessageBox.Show("A aquest servei li passa algo raro");
            }

            //foreach (var pref in preferencies_serveis)
            //    Context.Projecte.Serveis[pref.Key].Preferencies = pref.Value;

            /*var ass = AssignarServeisBuckets1(tenir_en_compte_data_ultima_visita, data_inici, data_fi, true);
            foreach (var kv in ass)
                if (assignats.ContainsKey(kv.Key)) assignats[kv.Key] += ass[kv.Key];
                else assignats.Add(kv.Key, kv.Value);*/

            return assignats;
        }

        private Dictionary<string, int> AssignarServeisBuckets(bool tenir_en_compte_data_ultima_visita, DateTime data_inici, DateTime data_fi, int mode = 1)
        {
            Dictionary<string, int> assignats = new Dictionary<string, int>();
            llista_serveis = OrdenarPerPreferencies(llista_serveis);
            for (int iis = 0; iis < llista_serveis.Count; iis++)
            {
                var s = llista_serveis[iis];
                if (mode == 1)
                {
                    if (s.Frequencia.TotalDays > 10) continue;
                }
                else
                {
                    if (s.Frequencia.TotalDays <= 10) continue;
                }

                if (s.Identificador == "602bc3bf-ea35-4077-bcdf-3e3b3f9f94b6" || s.Identificador == "1560e499-2905-481f-9af2-e2ce7d8adfad")
                    s.ToString();

                string zona_servei = Context.AssignarZona(s);
                List<Tuple<DateTime, int>> dies;
                bool visita_adicional = false;

                DateTime d = CalcularUltimaVisita(s, tenir_en_compte_data_ultima_visita, data_inici, data_fi);
                if (mode == 2)
                    d = s.UltimaVisita;

                Context.PlanificarVisitesServeiComplet(s, d, data_inici, data_fi, out dies);

                Dictionary<int, int> ns = new Dictionary<int, int>();
                foreach (var t in dies) if (!ns.ContainsKey(t.Item2)) ns.Add(t.Item2, 1); else ns[t.Item2]++;
                if (ns.ContainsKey(1) && !ns.ContainsKey(0) && !ns.ContainsKey(2) && !ns.ContainsKey(3) && !ns.ContainsKey(4))
                    continue;
                var zones = Context.Zones();

                if (ns.ContainsKey(4) && !ns.ContainsKey(0) && !ns.ContainsKey(2) && !ns.ContainsKey(3))
                {
                    // Servei obligatori
                    #region Obligatoris
                    List<DateTime> dies_visita = new List<DateTime>();
                    foreach (var t in dies) if (t.Item2 == 4) dies_visita.Add(t.Item1);
                    //n2 += dies_visita.Count;
                    foreach (var dia in dies_visita)
                    {
                        // Per cada dia que s'hagi de planificar aquest servei
                        bool assignat = false;

                        // Busco dels buckets del seu CP i dia, el més proper
                        List<Tuple<Bucket, double>> buckets_dia = new List<Tuple<Bucket, double>>();
                        foreach (var b in serveis_buckets[s.Identificador])
                            if (b.Dia.Value == dia)
                                buckets_dia.Add(new Tuple<Bucket, double>(b, b.DistanciaServei(s)));
                        buckets_dia.Sort(new Comparison<Tuple<Bucket, double>>((a, b) => Math.Sign(a.Item2 - b.Item2)));
                        foreach (var tup in buckets_dia)
                            if (tup.Item1.AssignarServei(s, buckets) == 0)
                            {
                                assignat = true;
                                string tipus = "OB_CP_OK"; if (!assignats.ContainsKey(tipus)) assignats.Add(tipus, 1); else assignats[tipus]++;
                                break;
                            }

                        // Si no puc assignar-lo a les bones, l'assigno a les males
                        if (!assignat)
                            if (buckets_dia.Count > 0)
                                if (buckets_dia[0].Item1.AssignarServei(s, buckets, true) == 0)
                                {
                                    assignat = true;
                                    string tipus = "OB_CP_Forsant"; if (!assignats.ContainsKey(tipus)) assignats.Add(tipus, 1); else assignats[tipus]++;
                                }

                        // Si no puc ni a les males, busco el bucket més proper del seu dia
                        if (!assignat)
                        {
                            buckets_dia = new List<Tuple<Bucket, double>>();
                            foreach (var b in buckets)
                                if (b.Dia.Value == dia)
                                    buckets_dia.Add(new Tuple<Bucket, double>(b, b.DistanciaServei(s)));
                            buckets_dia.Sort(new Comparison<Tuple<Bucket, double>>((a, b) => Math.Sign(a.Item2 - b.Item2)));
                            foreach (var tup in buckets_dia)
                                if (tup.Item1.AssignarServei(s, buckets) == 0)
                                {
                                    assignat = true;
                                    string tipus = "OB_NOCP"; if (!assignats.ContainsKey(tipus)) assignats.Add(tipus, 1); else assignats[tipus]++;
                                    break;
                                }
                        }

                        if (!assignat)
                        {
                            string tipus = "OB_KO"; if (!assignats.ContainsKey(tipus)) assignats.Add(tipus, 1); else assignats[tipus]++;
                        }
                    }
                    #endregion
                }
                else if (ns.ContainsKey(2) || ns.ContainsKey(3))
                {
                    // Servei no obligatori
                    #region No Obligatoris
                    List<DateTime> dies_visita = new List<DateTime>(), dies2 = new List<DateTime>();
                    if (visita_adicional)
                        dies_visita.Add(data_inici);
                    foreach (var t in dies)
                        if (t.Item2 >= 3)
                        {
                            if (!dies_visita.Contains(t.Item1))
                                dies_visita.Add(t.Item1);
                        }
                        else if (t.Item2 == 2) dies2.Add(t.Item1);

                    for (int idia = 0; idia < dies_visita.Count; idia++)
                    {
                        var dia = dies_visita[idia];
                        bool assignat = false;
                        List<Tuple<Bucket, double>> buckets_dia = new List<Tuple<Bucket, double>>();

                        if (mode == 1)
                        {
                            // Puntuo dies, segons preferència i proximitat
                            foreach (var b in serveis_buckets[s.Identificador])
                                if (b.Dia.Value == dia)
                                {
                                    double puntuacio = 1000.0 - b.DistanciaServei(s);
                                    buckets_dia.Add(new Tuple<Bucket, double>(b, puntuacio));
                                }
                            int marge = 1;
                            while (true)
                            {
                                foreach (var b in serveis_buckets[s.Identificador])
                                {
                                    var d2 = SumaDiesLaborals(dia, marge);
                                    var d3 = SumaDiesLaborals(dia, -marge);
                                    if ((b.Dia.Value == d2 && dies2.Contains(d2)) || (b.Dia.Value == d3 && dies2.Contains(d3)))
                                    {
                                        bool hies = false;
                                        foreach (var s1 in b.serveis)
                                            if (s1.Identificador == s.Identificador) { hies = true; break; }
                                        if (hies) continue;
                                        foreach (var bb in buckets)
                                        {
                                            if (bb.Dia.Value == b.Dia.Value && b.zona == bb.zona && bb != b)
                                                foreach (var s1 in bb.serveis) if (s1.Identificador == s.Identificador) { hies = true; break; }
                                            if (hies) break;
                                        }
                                        if (hies) continue;
                                        double puntuacio = 1000.0 - marge * 100 - b.DistanciaServei(s);
                                        buckets_dia.Add(new Tuple<Bucket, double>(b, puntuacio));
                                    }
                                }
                                marge++;
                                var d22 = SumaDiesLaborals(dia, marge);
                                var d33 = SumaDiesLaborals(dia, -marge);
                                //if (!dies2.Contains(d22) && !dies2.Contains(d33)) break;
                                if (marge > s.Marge.TotalDays) break;
                            }

                            buckets_dia.Sort(new Comparison<Tuple<Bucket, double>>((a, b) => Math.Sign(b.Item2 - a.Item2)));
                            foreach (var tup in buckets_dia)
                                if (tup.Item1.AssignarServei(s, buckets) == 0)
                                {
                                    var dif = (tup.Item1.Dia.Value - dia).TotalDays;
                                    for (int jdia = idia + 1; jdia < dies_visita.Count; jdia++) dies_visita[jdia] = dies_visita[jdia].AddDays((int)dif);
                                    assignat = true;
                                    string tipus = "NOOB_CP_OK"; if (!assignats.ContainsKey(tipus)) assignats.Add(tipus, 1); else assignats[tipus]++;
                                    break;
                                }

                            if (!assignat)
                            {
                                foreach (var tup in buckets_dia)
                                    if (tup.Item1.AssignarServei(s, buckets, true) == 0)
                                    {
                                        var dif = (tup.Item1.Dia.Value - dia).TotalDays;
                                        for (int jdia = idia + 1; jdia < dies_visita.Count; jdia++) dies_visita[jdia] = dies_visita[jdia].AddDays((int)dif);
                                        assignat = true;
                                        string tipus = "NOOB_CP_OK_Forçant"; if (!assignats.ContainsKey(tipus)) assignats.Add(tipus, 1); else assignats[tipus]++;
                                        break;
                                    }
                            }

                            if (!assignat && s.Frequencia.TotalDays == 5 && serveis_buckets[s.Identificador].Count > 0)
                            {
                                // Amb els setmanals, buscar el dia del bucket que es vagi al seu CP encara que no sigui de la setmana que li toca, i escollir bucket només segons el dia
                                var dia_setmana = serveis_buckets[s.Identificador].First().Dia.Value.DayOfWeek;
                                buckets_dia = new List<Tuple<Bucket, double>>();
                                foreach (var b in buckets)
                                {
                                    if (b.Dia.Value.DayOfWeek != dia_setmana) continue;
                                    bool correcte = false;
                                    if (Context.Setmana(b.Dia.Value) == Context.Setmana(dia)) correcte = true;
                                    else if (dies2.Contains(b.Dia.Value))
                                    {
                                        if (SumaDiesLaborals(dia, (int)s.Marge.TotalDays) >= b.Dia.Value && SumaDiesLaborals(dia, (int)-s.Marge.TotalDays) <= b.Dia.Value)
                                            correcte = true;
                                    }
                                    if (correcte)
                                    {
                                        bool hies = false;
                                        foreach (var s1 in b.serveis)
                                            if (s1.Identificador == s.Identificador) { hies = true; break; }
                                        if (hies) continue;
                                        foreach (var bb in buckets)
                                        {
                                            if (bb.Dia.Value == b.Dia.Value && b.zona == bb.zona && bb != b)
                                                foreach (var s1 in bb.serveis) if (s1.Identificador == s.Identificador) { hies = true; break; }
                                            if (hies) break;
                                        }
                                        if (hies) continue;
                                        int dif_dies = (int)Math.Abs((dia - b.Dia.Value).TotalDays);
                                        var val = b.DistanciaServei(s) + dif_dies;
                                        if (b.zona != zona_servei) val += 1000;
                                        buckets_dia.Add(new Tuple<Bucket, double>(b, val));
                                    }
                                }
                                buckets_dia.Sort(new Comparison<Tuple<Bucket, double>>((a, b) => Math.Sign(a.Item2 - b.Item2)));
                                foreach (var tup in buckets_dia)
                                    if (tup.Item1.AssignarServei(s, buckets) == 0)
                                    {
                                        var dif = (tup.Item1.Dia.Value - dia).TotalDays;
                                        for (int jdia = idia + 1; jdia < dies_visita.Count; jdia++) dies_visita[jdia] = dies_visita[jdia].AddDays((int)dif);
                                        assignat = true;
                                        string tipus = "NOOBS_Bucket_Opcional_Zona";
                                        if (serveis_buckets[s.Identificador].Count == 0) tipus = "NOOBS_Bucket_SenseCP_Zona";
                                        if (!assignats.ContainsKey(tipus)) assignats.Add(tipus, 1); else assignats[tipus]++;
                                        break;
                                    }
                            }

                            // Si no he pogut assignar-lo, provo amb els buckets de la seva zona i dels dies opcionals (primer els més propers)
                            if (!assignat)
                            {
                                buckets_dia = new List<Tuple<Bucket, double>>();
                                foreach (var b in buckets)
                                {
                                    if (b.zona != zona_servei) continue;
                                    bool correcte = false;
                                    if (b.Dia.Value == dia) correcte = true;
                                    else if (dies2.Contains(b.Dia.Value))
                                    {
                                        if (SumaDiesLaborals(dia, (int)s.Marge.TotalDays) >= b.Dia.Value && SumaDiesLaborals(dia, (int)-s.Marge.TotalDays) <= b.Dia.Value)
                                            correcte = true;
                                    }
                                    if (correcte)
                                    {
                                        bool hies = false;
                                        foreach (var s1 in b.serveis)
                                            if (s1.Identificador == s.Identificador) { hies = true; break; }
                                        if (hies) continue;
                                        foreach (var bb in buckets)
                                        {
                                            if (bb.Dia.Value == b.Dia.Value && b.zona == bb.zona && bb != b)
                                                foreach (var s1 in bb.serveis) if (s1.Identificador == s.Identificador) { hies = true; break; }
                                            if (hies) break;
                                        }
                                        if (hies) continue;
                                        int dif_dies = (int)Math.Abs((dia - b.Dia.Value).TotalDays);
                                        buckets_dia.Add(new Tuple<Bucket, double>(b, b.DistanciaServei(s) + dif_dies));
                                    }
                                }
                                buckets_dia.Sort(new Comparison<Tuple<Bucket, double>>((a, b) => Math.Sign(a.Item2 - b.Item2)));
                                foreach (var tup in buckets_dia)
                                    if (tup.Item1.AssignarServei(s, buckets) == 0)
                                    {
                                        var dif = (tup.Item1.Dia.Value - dia).TotalDays;
                                        for (int jdia = idia + 1; jdia < dies_visita.Count; jdia++) dies_visita[jdia] = dies_visita[jdia].AddDays((int)dif);
                                        assignat = true;
                                        string tipus = "NOOB_Opcional_Zona";
                                        if (serveis_buckets[s.Identificador].Count == 0) tipus = "NOOB_SenseCP_Zona";
                                        if (!assignats.ContainsKey(tipus)) assignats.Add(tipus, 1); else assignats[tipus]++;
                                        break;
                                    }
                            }

                            // Si no, a les males amb tots els buckets opcionals de la seva zona, ordenant per número de serveis
                            if (!assignat)
                            {
                                buckets_dia = new List<Tuple<Bucket, double>>();
                                foreach (var b in buckets)
                                {
                                    if (b.zona != zona_servei) continue;
                                    bool correcte = false;
                                    if (b.Dia.Value == dia) correcte = true;
                                    else if (dies2.Contains(b.Dia.Value))
                                    {
                                        if (SumaDiesLaborals(dia, (int)s.Marge.TotalDays) >= b.Dia.Value && SumaDiesLaborals(dia, (int)-s.Marge.TotalDays) <= b.Dia.Value)
                                            correcte = true;
                                    }
                                    if (correcte)
                                    {
                                        bool hies = false;
                                        foreach (var s1 in b.serveis)
                                            if (s1.Identificador == s.Identificador) { hies = true; break; }
                                        if (hies) continue;
                                        foreach (var bb in buckets)
                                        {
                                            if (bb.Dia.Value == b.Dia.Value && b.zona == bb.zona && bb != b)
                                                foreach (var s1 in bb.serveis) if (s1.Identificador == s.Identificador) { hies = true; break; }
                                            if (hies) break;
                                        }
                                        if (hies) continue;
                                        buckets_dia.Add(new Tuple<Bucket, double>(b, b.DistanciaServei(s)));
                                    }
                                }
                                buckets_dia.Sort(new Comparison<Tuple<Bucket, double>>((a, b) => Math.Sign(a.Item1.serveis.Count - b.Item1.serveis.Count)));
                                foreach (var tup in buckets_dia)
                                    if (tup.Item1.AssignarServei(s, buckets, true) == 0)
                                    {
                                        var dif = (tup.Item1.Dia.Value - dia).TotalDays;
                                        for (int jdia = idia + 1; jdia < dies_visita.Count; jdia++) dies_visita[jdia] = dies_visita[jdia].AddDays((int)dif);
                                        assignat = true;
                                        string tipus = "NOOB_Opcional_Zona_Forsant";
                                        if (serveis_buckets[s.Identificador].Count == 0) tipus = "NOOB_SenseCP_Zona_Forsant";
                                        if (!assignats.ContainsKey(tipus)) assignats.Add(tipus, 1); else assignats[tipus]++;
                                        break;
                                    }
                            }

                            // Si encara no, provo amb els buckets que no són de la seva zona i de tots els dies opcionals, per proximiat
                            if (!assignat)
                            {
                                buckets_dia = new List<Tuple<Bucket, double>>();
                                foreach (var b in buckets)
                                {
                                    bool correcte = false;
                                    if (b.Dia.Value == dia) correcte = true;
                                    else if (dies2.Contains(b.Dia.Value))
                                    {
                                        if (SumaDiesLaborals(dia, (int)s.Marge.TotalDays) >= b.Dia.Value && SumaDiesLaborals(dia, (int)-s.Marge.TotalDays) <= b.Dia.Value)
                                            correcte = true;
                                    }
                                    if (correcte)
                                    {
                                        bool hies = false;
                                        foreach (var s1 in b.serveis)
                                            if (s1.Identificador == s.Identificador) { hies = true; break; }
                                        if (hies) continue;
                                        foreach (var bb in buckets)
                                        {
                                            if (bb.Dia.Value == b.Dia.Value && b.zona == bb.zona && bb != b)
                                                foreach (var s1 in bb.serveis) if (s1.Identificador == s.Identificador) { hies = true; break; }
                                            if (hies) break;
                                        }
                                        if (hies) continue;
                                        buckets_dia.Add(new Tuple<Bucket, double>(b, b.DistanciaServei(s)));
                                    }
                                }
                                buckets_dia.Sort(new Comparison<Tuple<Bucket, double>>((a, b) => Math.Sign(a.Item2 - b.Item2)));
                                foreach (var tup in buckets_dia)
                                    if (tup.Item1.AssignarServei(s, buckets) == 0)
                                    {
                                        var dif = (tup.Item1.Dia.Value - dia).TotalDays;
                                        for (int jdia = idia + 1; jdia < dies_visita.Count; jdia++) dies_visita[jdia] = dies_visita[jdia].AddDays((int)dif);
                                        assignat = true;
                                        string tipus = "NOOB_Opcional_NOCP_Nozona";
                                        if (serveis_buckets[s.Identificador].Count == 0) tipus = "NOOB_SenseCP_Nozona";
                                        if (!assignats.ContainsKey(tipus)) assignats.Add(tipus, 1); else assignats[tipus]++;
                                        break;
                                    }
                            }
                        }
                        else
                        {
                            buckets_dia = new List<Tuple<Bucket, double>>();
                            foreach (var b in buckets)
                            {
                                if (b.zona != zona_servei) continue;
                                bool correcte = false;
                                if (b.Dia.Value == dia) correcte = true;
                                else if (dies2.Contains(b.Dia.Value))
                                {
                                    if (SumaDiesLaborals(dia, (int)s.Marge.TotalDays) >= b.Dia.Value && SumaDiesLaborals(dia, (int)-s.Marge.TotalDays) <= b.Dia.Value)
                                        correcte = true;
                                }
                                if (correcte)
                                {
                                    bool hies = false;
                                    foreach (var s1 in b.serveis)
                                        if (s1.Identificador == s.Identificador) { hies = true; break; }
                                    if (hies) continue;
                                    foreach (var bb in buckets)
                                    {
                                        if (bb.Dia.Value == b.Dia.Value && b.zona == bb.zona && bb != b)
                                            foreach (var s1 in bb.serveis) if (s1.Identificador == s.Identificador) { hies = true; break; }
                                        if (hies) break;
                                    }
                                    if (hies) continue;
                                    int dif_dies = (int)Math.Abs((dia - b.Dia.Value).TotalDays);
                                    var puntuacio = b.DistanciaServei(s);
                                    if (s.Marge.TotalDays < 5) puntuacio += dif_dies / 2;
                                    if (b.serveis.Count > b.CarregaTotal() * zones[b.zona].NServeis) puntuacio += b.serveis.Count - b.CarregaTotal() * zones[b.zona].NServeis;
                                    buckets_dia.Add(new Tuple<Bucket, double>(b, puntuacio));
                                }
                            }
                            buckets_dia.Sort(new Comparison<Tuple<Bucket, double>>((a, b) => Math.Sign(a.Item2 - b.Item2)));
                            foreach (var tup in buckets_dia)
                                if (tup.Item1.AssignarServei(s, buckets) == 0)
                                {
                                    var dif = (tup.Item1.Dia.Value - dia).TotalDays;
                                    for (int jdia = idia + 1; jdia < dies_visita.Count; jdia++) dies_visita[jdia] = dies_visita[jdia].AddDays((int)dif);
                                    assignat = true;
                                    string tipus = "NOOB_Opcional_Zona";
                                    if (serveis_buckets[s.Identificador].Count == 0) tipus = "NOOB_SenseCP_Zona";
                                    if (!assignats.ContainsKey(tipus)) assignats.Add(tipus, 1); else assignats[tipus]++;
                                    break;
                                }
                            if (!assignat)
                            {
                                assignat.ToString();
                            }
                        }

                        if (!assignat)
                        {
                            // No hi ha maneres
                            string tipus = "NOOB_KO"; if (!assignats.ContainsKey(tipus)) assignats.Add(tipus, 1); else assignats[tipus]++;
                        }
                    }
                    #endregion
                }
                else
                    MessageBox.Show("A aquest servei li passa algo raro");
            }

            if (mode == 1)
            {
                var ass = AssignarServeisBuckets(tenir_en_compte_data_ultima_visita, data_inici, data_fi, 2);
                foreach (var kv in ass)
                    if (assignats.ContainsKey(kv.Key)) assignats[kv.Key] += ass[kv.Key];
                    else assignats.Add(kv.Key, kv.Value);
            }

            List<Tuple<Bucket, Servei, double>> l = new List<Tuple<Bucket, Servei, double>>();
            foreach (var b in buckets)
                foreach (var s in b.serveis)
                    if (!b.ConteCP(s.CodiPostal))
                    {
                        var dist = b.DistanciaServei2(s);
                        l.Add(Tuple.Create(b, s, dist));
                    }
            l.Sort(new Comparison<Tuple<Bucket, Servei, double>>((a, b) => Math.Sign(a.Item3 - b.Item3)));
            //foreach (var t in l)
            //{
            //    var f = new StreamWriter(nom_fitxer_log, true); f.WriteLine("Servei fora bucket. Servei: " + t.Item2.Nom + ". Dia " + t.Item1.Dia.Value.ToShortDateString() + ". Zona " + t.Item1.zona + ". Distància: " + Math.Round(t.Item3, 3)); f.Close();
            //}

            return assignats;
        }

        private Dictionary<string, int> AssignarServeisBuckets0(bool tenir_en_compte_data_ultima_visita, DateTime data_inici, DateTime data_fi, bool nomes_mensuals_o_mes = false)
        {
            Dictionary<string, int> assignats = new Dictionary<string, int>();
            llista_serveis = OrdenarPerPreferencies(llista_serveis);
            for (int iis = 0; iis < llista_serveis.Count; iis++)
            {
                var s = llista_serveis[iis];
                //if (nomes_mensuals_o_mes && s.Frequencia.TotalDays <= 20) continue; // Només bimensuals i trimestrals

                string zona_servei = Context.AssignarZona(s);
                List<Tuple<DateTime, int>> dies;
                bool visita_adicional = false;

                DateTime d = CalcularUltimaVisita(s, tenir_en_compte_data_ultima_visita, data_inici, data_fi);
                //d = s.UltimaVisita;

                Context.PlanificarVisitesServeiComplet(s, d, data_inici, data_fi, out dies);

                Dictionary<int, int> ns = new Dictionary<int, int>();
                foreach (var t in dies) if (!ns.ContainsKey(t.Item2)) ns.Add(t.Item2, 1); else ns[t.Item2]++;
                if (ns.ContainsKey(1) && !ns.ContainsKey(0) && !ns.ContainsKey(2) && !ns.ContainsKey(3) && !ns.ContainsKey(4))
                    continue;

                if (ns.ContainsKey(4) && !ns.ContainsKey(0) && !ns.ContainsKey(2) && !ns.ContainsKey(3))
                {
                    // Servei obligatori
                    #region Obligatoris
                    List<DateTime> dies_visita = new List<DateTime>();
                    foreach (var t in dies) if (t.Item2 == 4) dies_visita.Add(t.Item1);
                    //n2 += dies_visita.Count;
                    foreach (var dia in dies_visita)
                    {
                        // Per cada dia que s'hagi de planificar aquest servei
                        bool assignat = false;

                        // Busco dels buckets del seu CP i dia, el més proper
                        List<Tuple<Bucket, double>> buckets_dia = new List<Tuple<Bucket, double>>();
                        foreach (var b in serveis_buckets[s.Identificador])
                            if (b.Dia.Value == dia)
                                buckets_dia.Add(new Tuple<Bucket, double>(b, b.DistanciaServei(s)));
                        buckets_dia.Sort(new Comparison<Tuple<Bucket, double>>((a, b) => Math.Sign(a.Item2 - b.Item2)));
                        foreach (var tup in buckets_dia)
                            if (tup.Item1.AssignarServei(s, buckets) == 0)
                            {
                                assignat = true;
                                string tipus = "OB_CP_OK"; if (!assignats.ContainsKey(tipus)) assignats.Add(tipus, 1); else assignats[tipus]++;
                                break;
                            }

                        // Si no puc assignar-lo a les bones, l'assigno a les males
                        if (!assignat)
                            if (buckets_dia.Count > 0)
                                if (buckets_dia[0].Item1.AssignarServei(s, buckets, true) == 0)
                                {
                                    assignat = true;
                                    string tipus = "OB_CP_Forsant"; if (!assignats.ContainsKey(tipus)) assignats.Add(tipus, 1); else assignats[tipus]++;
                                }

                        // Si no puc ni a les males, busco el bucket més proper del seu dia
                        if (!assignat)
                        {
                            buckets_dia = new List<Tuple<Bucket, double>>();
                            foreach (var b in buckets)
                                if (b.Dia.Value == dia)
                                    buckets_dia.Add(new Tuple<Bucket, double>(b, b.DistanciaServei(s)));
                            buckets_dia.Sort(new Comparison<Tuple<Bucket, double>>((a, b) => Math.Sign(a.Item2 - b.Item2)));
                            foreach (var tup in buckets_dia)
                                if (tup.Item1.AssignarServei(s, buckets) == 0)
                                {
                                    assignat = true;
                                    string tipus = "OB_NOCP"; if (!assignats.ContainsKey(tipus)) assignats.Add(tipus, 1); else assignats[tipus]++;
                                    break;
                                }
                        }

                        if (!assignat)
                        {
                            string tipus = "OB_KO"; if (!assignats.ContainsKey(tipus)) assignats.Add(tipus, 1); else assignats[tipus]++;
                        }
                    }
                    #endregion
                }
                else if (ns.ContainsKey(2) || ns.ContainsKey(3))
                {
                    // Servei no obligatori
                    #region No Obligatoris
                    List<DateTime> dies_visita = new List<DateTime>(), dies2 = new List<DateTime>();
                    if (visita_adicional)
                        dies_visita.Add(data_inici);
                    foreach (var t in dies)
                        if (t.Item2 >= 3)
                        {
                            if (!dies_visita.Contains(t.Item1))
                                dies_visita.Add(t.Item1);
                        }
                        else if (t.Item2 == 2) dies2.Add(t.Item1);

                    for (int idia = 0; idia < dies_visita.Count; idia++)
                    {
                        var dia = dies_visita[idia];
                        bool assignat = false;

                        // Puntuo dies, segons preferència i proximitat
                        List<Tuple<Bucket, double>> buckets_dia = new List<Tuple<Bucket, double>>();

                        foreach (var b in serveis_buckets[s.Identificador])
                            if (b.Dia.Value == dia)
                            {
                                double puntuacio = 1000.0 - b.DistanciaServei(s);
                                buckets_dia.Add(new Tuple<Bucket, double>(b, puntuacio));
                            }
                        int marge = 1;
                        while (true)
                        {
                            foreach (var b in serveis_buckets[s.Identificador])
                            {
                                var d2 = SumaDiesLaborals(dia, marge);
                                var d3 = SumaDiesLaborals(dia, -marge);
                                if ((b.Dia.Value == d2 && dies2.Contains(d2)) || (b.Dia.Value == d3 && dies2.Contains(d3)))
                                {
                                    bool hies = false;
                                    foreach (var s1 in b.serveis)
                                        if (s1.Identificador == s.Identificador) { hies = true; break; }
                                    if (hies) continue;
                                    foreach (var bb in buckets)
                                    {
                                        if (bb.Dia.Value == b.Dia.Value && b.zona == bb.zona && bb != b)
                                            foreach (var s1 in bb.serveis) if (s1.Identificador == s.Identificador) { hies = true; break; }
                                        if (hies) break;
                                    }
                                    if (hies) continue;
                                    double puntuacio = 1000.0 - marge * 100 - b.DistanciaServei(s);
                                    buckets_dia.Add(new Tuple<Bucket, double>(b, puntuacio));
                                }
                            }
                            marge++;
                            var d22 = SumaDiesLaborals(dia, marge);
                            var d33 = SumaDiesLaborals(dia, -marge);
                            //if (!dies2.Contains(d22) && !dies2.Contains(d33)) break;
                            if (marge > s.Marge.TotalDays) break;
                        }

                        buckets_dia.Sort(new Comparison<Tuple<Bucket, double>>((a, b) => Math.Sign(b.Item2 - a.Item2)));
                        foreach (var tup in buckets_dia)
                            if (tup.Item1.AssignarServei(s, buckets) == 0)
                            {
                                var dif = (tup.Item1.Dia.Value - dia).TotalDays;
                                for (int jdia = idia + 1; jdia < dies_visita.Count; jdia++) dies_visita[jdia] = dies_visita[jdia].AddDays((int)dif);
                                assignat = true;
                                string tipus = "NOOB_CP_OK"; if (!assignats.ContainsKey(tipus)) assignats.Add(tipus, 1); else assignats[tipus]++;
                                break;
                            }

                        if (!assignat)
                        {
                            foreach (var tup in buckets_dia)
                                if (tup.Item1.AssignarServei(s, buckets, true) == 0)
                                {
                                    var dif = (tup.Item1.Dia.Value - dia).TotalDays;
                                    for (int jdia = idia + 1; jdia < dies_visita.Count; jdia++) dies_visita[jdia] = dies_visita[jdia].AddDays((int)dif);
                                    assignat = true;
                                    string tipus = "NOOB_CP_OK_Forçant"; if (!assignats.ContainsKey(tipus)) assignats.Add(tipus, 1); else assignats[tipus]++;
                                    break;
                                }
                        }

                        if (!assignat && s.Frequencia.TotalDays == 5 && serveis_buckets[s.Identificador].Count > 0)
                        {
                            // Amb els setmanals, buscar el dia del bucket que es vagi al seu CP encara que no sigui de la setmana que li toca, i escollir bucket només segons el dia
                            var dia_setmana = serveis_buckets[s.Identificador].First().Dia.Value.DayOfWeek;
                            buckets_dia = new List<Tuple<Bucket, double>>();
                            foreach (var b in buckets)
                            {
                                if (b.Dia.Value.DayOfWeek != dia_setmana) continue;
                                bool correcte = false;
                                if (b.Dia.Value == dia) correcte = true;
                                else if (dies2.Contains(b.Dia.Value))
                                {
                                    if (SumaDiesLaborals(dia, (int)s.Marge.TotalDays) >= b.Dia.Value && SumaDiesLaborals(dia, (int)-s.Marge.TotalDays) <= b.Dia.Value)
                                        correcte = true;
                                }
                                if (correcte)
                                {
                                    bool hies = false;
                                    foreach (var s1 in b.serveis)
                                        if (s1.Identificador == s.Identificador) { hies = true; break; }
                                    if (hies) continue;
                                    foreach (var bb in buckets)
                                    {
                                        if (bb.Dia.Value == b.Dia.Value && b.zona == bb.zona && bb != b)
                                            foreach (var s1 in bb.serveis) if (s1.Identificador == s.Identificador) { hies = true; break; }
                                        if (hies) break;
                                    }
                                    if (hies) continue;
                                    int dif_dies = (int)Math.Abs((dia - b.Dia.Value).TotalDays);
                                    var val = b.DistanciaServei(s) + dif_dies;
                                    if (b.zona != zona_servei) val += 1000;
                                    buckets_dia.Add(new Tuple<Bucket, double>(b, val));
                                }
                            }
                            buckets_dia.Sort(new Comparison<Tuple<Bucket, double>>((a, b) => Math.Sign(a.Item2 - b.Item2)));
                            foreach (var tup in buckets_dia)
                                if (tup.Item1.AssignarServei(s, buckets) == 0)
                                {
                                    var dif = (tup.Item1.Dia.Value - dia).TotalDays;
                                    for (int jdia = idia + 1; jdia < dies_visita.Count; jdia++) dies_visita[jdia] = dies_visita[jdia].AddDays((int)dif);
                                    assignat = true;
                                    string tipus = "NOOBS_Bucket_Opcional_Zona";
                                    if (serveis_buckets[s.Identificador].Count == 0) tipus = "NOOBS_Bucket_SenseCP_Zona";
                                    if (!assignats.ContainsKey(tipus)) assignats.Add(tipus, 1); else assignats[tipus]++;
                                    break;
                                }
                        }

                        // Si no he pogut assignar-lo, provo amb els buckets de la seva zona i dels dies opcionals (primer els més propers)
                        if (!assignat)
                        {
                            buckets_dia = new List<Tuple<Bucket, double>>();
                            foreach (var b in buckets)
                            {
                                if (b.zona != zona_servei) continue;
                                bool correcte = false;
                                if (b.Dia.Value == dia) correcte = true;
                                else if (dies2.Contains(b.Dia.Value))
                                {
                                    if (SumaDiesLaborals(dia, (int)s.Marge.TotalDays) >= b.Dia.Value && SumaDiesLaborals(dia, (int)-s.Marge.TotalDays) <= b.Dia.Value)
                                        correcte = true;
                                }
                                if (correcte)
                                {
                                    bool hies = false;
                                    foreach (var s1 in b.serveis)
                                        if (s1.Identificador == s.Identificador) { hies = true; break; }
                                    if (hies) continue;
                                    foreach (var bb in buckets)
                                    {
                                        if (bb.Dia.Value == b.Dia.Value && b.zona == bb.zona && bb != b)
                                            foreach (var s1 in bb.serveis) if (s1.Identificador == s.Identificador) { hies = true; break; }
                                        if (hies) break;
                                    }
                                    if (hies) continue;
                                    int dif_dies = (int)Math.Abs((dia - b.Dia.Value).TotalDays);
                                    buckets_dia.Add(new Tuple<Bucket, double>(b, b.DistanciaServei(s) + dif_dies));
                                }
                            }
                            buckets_dia.Sort(new Comparison<Tuple<Bucket, double>>((a, b) => Math.Sign(a.Item2 - b.Item2)));
                            foreach (var tup in buckets_dia)
                                if (tup.Item1.AssignarServei(s, buckets) == 0)
                                {
                                    var dif = (tup.Item1.Dia.Value - dia).TotalDays;
                                    for (int jdia = idia + 1; jdia < dies_visita.Count; jdia++) dies_visita[jdia] = dies_visita[jdia].AddDays((int)dif);
                                    assignat = true;
                                    string tipus = "NOOB_Opcional_Zona";
                                    if (serveis_buckets[s.Identificador].Count == 0) tipus = "NOOB_SenseCP_Zona";
                                    if (!assignats.ContainsKey(tipus)) assignats.Add(tipus, 1); else assignats[tipus]++;
                                    break;
                                }
                        }

                        // Si no, a les males amb tots els buckets opcionals de la seva zona, ordenant per número de serveis
                        if (!assignat)
                        {
                            buckets_dia = new List<Tuple<Bucket, double>>();
                            foreach (var b in buckets)
                            {
                                if (b.zona != zona_servei) continue;
                                bool correcte = false;
                                if (b.Dia.Value == dia) correcte = true;
                                else if (dies2.Contains(b.Dia.Value))
                                {
                                    if (SumaDiesLaborals(dia, (int)s.Marge.TotalDays) >= b.Dia.Value && SumaDiesLaborals(dia, (int)-s.Marge.TotalDays) <= b.Dia.Value)
                                        correcte = true;
                                }
                                if (correcte)
                                {
                                    bool hies = false;
                                    foreach (var s1 in b.serveis)
                                        if (s1.Identificador == s.Identificador) { hies = true; break; }
                                    if (hies) continue;
                                    foreach (var bb in buckets)
                                    {
                                        if (bb.Dia.Value == b.Dia.Value && b.zona == bb.zona && bb != b)
                                            foreach (var s1 in bb.serveis) if (s1.Identificador == s.Identificador) { hies = true; break; }
                                        if (hies) break;
                                    }
                                    if (hies) continue;
                                    buckets_dia.Add(new Tuple<Bucket, double>(b, b.DistanciaServei(s)));
                                }
                            }
                            buckets_dia.Sort(new Comparison<Tuple<Bucket, double>>((a, b) => Math.Sign(a.Item1.serveis.Count - b.Item1.serveis.Count)));
                            foreach (var tup in buckets_dia)
                                if (tup.Item1.AssignarServei(s, buckets, true) == 0)
                                {
                                    var dif = (tup.Item1.Dia.Value - dia).TotalDays;
                                    for (int jdia = idia + 1; jdia < dies_visita.Count; jdia++) dies_visita[jdia] = dies_visita[jdia].AddDays((int)dif);
                                    assignat = true;
                                    string tipus = "NOOB_Opcional_Zona_Forsant";
                                    if (serveis_buckets[s.Identificador].Count == 0) tipus = "NOOB_SenseCP_Zona_Forsant";
                                    if (!assignats.ContainsKey(tipus)) assignats.Add(tipus, 1); else assignats[tipus]++;
                                    break;
                                }
                        }

                        // Si encara no, provo amb els buckets que no són de la seva zona i de tots els dies opcionals, per proximiat
                        if (!assignat)
                        {
                            buckets_dia = new List<Tuple<Bucket, double>>();
                            foreach (var b in buckets)
                            {
                                bool correcte = false;
                                if (b.Dia.Value == dia) correcte = true;
                                else if (dies2.Contains(b.Dia.Value))
                                {
                                    if (SumaDiesLaborals(dia, (int)s.Marge.TotalDays) >= b.Dia.Value && SumaDiesLaborals(dia, (int)-s.Marge.TotalDays) <= b.Dia.Value)
                                        correcte = true;
                                }
                                if (correcte)
                                {
                                    bool hies = false;
                                    foreach (var s1 in b.serveis)
                                        if (s1.Identificador == s.Identificador) { hies = true; break; }
                                    if (hies) continue;
                                    foreach (var bb in buckets)
                                    {
                                        if (bb.Dia.Value == b.Dia.Value && b.zona == bb.zona && bb != b)
                                            foreach (var s1 in bb.serveis) if (s1.Identificador == s.Identificador) { hies = true; break; }
                                        if (hies) break;
                                    }
                                    if (hies) continue;
                                    buckets_dia.Add(new Tuple<Bucket, double>(b, b.DistanciaServei(s)));
                                }
                            }
                            buckets_dia.Sort(new Comparison<Tuple<Bucket, double>>((a, b) => Math.Sign(a.Item2 - b.Item2)));
                            foreach (var tup in buckets_dia)
                                if (tup.Item1.AssignarServei(s, buckets) == 0)
                                {
                                    var dif = (tup.Item1.Dia.Value - dia).TotalDays;
                                    for (int jdia = idia + 1; jdia < dies_visita.Count; jdia++) dies_visita[jdia] = dies_visita[jdia].AddDays((int)dif);
                                    assignat = true;
                                    string tipus = "NOOB_Opcional_NOCP_Nozona";
                                    if (serveis_buckets[s.Identificador].Count == 0) tipus = "NOOB_SenseCP_Nozona";
                                    if (!assignats.ContainsKey(tipus)) assignats.Add(tipus, 1); else assignats[tipus]++;
                                    break;
                                }
                        }

                        if (!assignat)
                        {
                            // No hi ha maneres
                            string tipus = "NOOB_KO"; if (!assignats.ContainsKey(tipus)) assignats.Add(tipus, 1); else assignats[tipus]++;
                        }
                    }
                    #endregion
                }
                else
                    MessageBox.Show("A aquest servei li passa algo raro");
            }

            return assignats;
        }

        private DateTime SumaDiesLaborals(DateTime d, int dies)
        {
            DateTime d2 = d;
            if (dies > 0)
            {
                //d2 = d2.AddDays(dies);
                
                for (int i = 0; i < dies; i++)
                {
                    d2 = d2.AddDays(1);
                    while (d2.DayOfWeek == DayOfWeek.Saturday || d2.DayOfWeek == DayOfWeek.Sunday)
                        d2 = d2.AddDays(1);
                }
            }
            else if (dies < 0)
            {
                //d2 = d2.AddDays(-dies);
                
                for (int i = 0; i < -dies; i++)
                {
                    d2 = d2.AddDays(-1);
                    while (d2.DayOfWeek == DayOfWeek.Saturday || d2.DayOfWeek == DayOfWeek.Sunday)
                        d2 = d2.AddDays(-1);
                }
            }
            return d2;
        }

        private void ControlServeisRepetitsDia()
        {
            foreach (var b1 in buckets)
            {
                HashSet<string> ids_b1 = new HashSet<string>();
                foreach (var s in b1.serveis)
                {
                    if (ids_b1.Contains(s.Identificador))
                    {
                        MessageBox.Show("Repe1 " + s.Identificador);
                        return;
                    }
                    else ids_b1.Add(s.Identificador);
                }
                foreach (var b2 in buckets)
                    if (b1 != b2)
                        if (b1.Dia.Value.ToShortDateString() == b2.Dia.Value.ToShortDateString())
                            foreach (var s2 in b2.serveis)
                                if (ids_b1.Contains(s2.Identificador))
                                {
                                    MessageBox.Show("Repe2 " + s2.Identificador);
                                    return;
                                }
            }
        }

        private void ControlServeisForaZona()
        {
            foreach (var b in buckets)
                foreach (var s in b.serveis)
                {
                    string zona = Context.AssignarZona(s);
                    if (zona != b.zona)
                        MessageBox.Show("Fora zona " + s.Identificador);
                }
        }


        private string FerString()
        {
            Dictionary<string, List<Bucket>> dic_zones_buckets = new Dictionary<string, List<Bucket>>();
            foreach (var b in buckets) if (!dic_zones_buckets.ContainsKey(b.zona)) dic_zones_buckets.Add(b.zona, new List<Bucket>() { b }); else dic_zones_buckets[b.zona].Add(b);
            Dictionary<string, int> nbuckets = new Dictionary<string, int>();
            Dictionary<string, List<Servei>> zonesbuckets = new Dictionary<string, List<Servei>>();
            foreach (var b in buckets)
            {
                string clau = b.Dia.Value.ToShortDateString() + ";" + b.zona;
                if (!zonesbuckets.ContainsKey(clau))
                {
                    zonesbuckets.Add(clau, new List<Servei>());
                    nbuckets.Add(clau, 0);
                }
                zonesbuckets[clau].AddRange(b.serveis);
                nbuckets[clau]++;
            }

            Dictionary<string, List<Tuple<DateTime, int, int>>> stri = new Dictionary<string, List<Tuple<DateTime, int, int>>>();
            List<DateTime> stridies = new List<DateTime>();
            Dictionary<string, int> suma_s = new Dictionary<string, int>();
            Dictionary<string, int> suma_sb = new Dictionary<string, int>();
            Dictionary<string, double> desvs_est = new Dictionary<string, double>();
            Dictionary<string, double> mitjanes = new Dictionary<string, double>();
            foreach (var z in zonesbuckets)
            {
                var zona = z.Key.Split(';')[1];
                var diad = z.Key.Split(';')[0];
                if (!stridies.Contains(DateTime.Parse(diad))) stridies.Add(DateTime.Parse(diad));
                if (!stri.ContainsKey(zona)) stri.Add(zona, new List<Tuple<DateTime, int, int>>());
                stri[zona].Add(new Tuple<DateTime, int, int>(DateTime.Parse(diad), z.Value.Count, nbuckets[z.Key]));
                if (!suma_s.ContainsKey(diad)) suma_s.Add(diad, 0); suma_s[diad] += z.Value.Count;
                if (!suma_sb.ContainsKey(diad)) suma_sb.Add(diad, 0); suma_sb[diad] += nbuckets[z.Key];
            }
            foreach (var z in dic_zones_buckets)
            {
                double desvest = 0;
                double mitjana = 0;
                foreach (var b in z.Value) mitjana += b.serveis.Count;
                mitjana /= (double)z.Value.Count;
                foreach (var b in z.Value) desvest += Math.Pow(b.serveis.Count - mitjana, 2);
                desvest = Math.Sqrt(desvest / (z.Value.Count - 1));
                mitjanes.Add(z.Key, mitjana);
                desvs_est.Add(z.Key, desvest);
            }
            stridies.Sort();
            string sstri = "\t";
            foreach (var sd in stridies) sstri += sd.Day + "\t";
            sstri += Environment.NewLine;
            double mitjana_serveis = 0;
            double mitjana_desviacions = 0;
            foreach (var zon in stri.Keys)
            {
                sstri += zon + '\t';
                foreach (var sd in stridies)
                {
                    foreach (var el in stri[zon])
                    {
                        if (el.Item1 == sd)
                        {
                            sstri += el.Item2 + "(" + el.Item3 + ")";
                            break;
                        }
                    }
                    sstri += "\t";
                }
                sstri += Math.Round(mitjanes[zon], 2) + "\t";
                sstri += Math.Round(desvs_est[zon], 2);
                mitjana_serveis += mitjanes[zon];
                mitjana_desviacions += desvs_est[zon];
                sstri += Environment.NewLine;
            }
            sstri += "Suma\t";
            double desvest_suma = 0;
            double mitjana_suma = 0;
            int n_suma = 0;
            foreach (var sd in stridies)
            {
                sstri += suma_s[sd.ToShortDateString()] + "(" + suma_sb[sd.ToShortDateString()] + ")";
                mitjana_suma += suma_s[sd.ToShortDateString()];
                n_suma++;
                sstri += "\t";
            }
            mitjana_suma /= (double)n_suma;
            foreach (var sd in stridies) desvest_suma += Math.Pow(suma_s[sd.ToShortDateString()] - mitjana_suma, 2);
            desvest_suma = Math.Sqrt(desvest_suma / (n_suma - 1));
            sstri += Math.Round(mitjana_suma, 2) + "\t";
            sstri += Math.Round(desvest_suma, 2);

            sstri += Environment.NewLine;
            sstri += "\t"; foreach (var sd in stridies) sstri += "\t";
            sstri += Math.Round(mitjana_serveis / stri.Keys.Count, 2) + "\t" + Math.Round(mitjana_desviacions / stri.Keys.Count, 2);
            return sstri;
        }

        private string StringAssignats(Dictionary<string, int> assignats)
        {
            string sass = "";
            foreach (var kv in assignats)
                sass += kv.Key + ": " + kv.Value + Environment.NewLine;
            /*sass += "Obligatoris -> CP : " + (assignats.ContainsKey("OB_CP_OK") ? assignats["OB_CP_OK"] : 0) + Environment.NewLine;
            sass += "Obligatoris -> CP Forçant : " + (assignats.ContainsKey("OB_CP_Forsant") ? assignats["OB_CP_Forsant"] : 0) + Environment.NewLine;
            sass += "Obligatoris -> No CP : " + (assignats.ContainsKey("OB_NOCP") ? assignats["OB_NOCP"] : 0) + Environment.NewLine;
            sass += "Obligatoris -> KO : " + (assignats.ContainsKey("OB_KO") ? assignats["OB_KO"] : 0) + Environment.NewLine;
            sass += "No Obligatoris -> CP OK : " + (assignats.ContainsKey("NOOB_CP_OK") ? assignats["NOOB_CP_OK"] : 0) + Environment.NewLine;
            sass += "No Obligatoris -> CP OK Forsant : " + (assignats.ContainsKey("NOOB_CP_OK_Forçant") ? assignats["NOOB_CP_OK_Forçant"] : 0) + Environment.NewLine;
            //sass += "No Obligatoris -> Dia Preferent CP : " + (assignats.ContainsKey("NOOB_Preferent_CP_OK") ? assignats["NOOB_Preferent_CP_OK"] : 0) + Environment.NewLine;
            //sass += "No Obligatoris -> Dia Opcional CP : " + (assignats.ContainsKey("NOOB_Opcional_CP_Marge") ? assignats["NOOB_Opcional_CP_Marge"] : 0) + Environment.NewLine;
            //sass += "No Obligatoris -> Dia Preferent CP Forsant : " + (assignats.ContainsKey("NOOB_Preferent_CP_Overflow") ? assignats["NOOB_Preferent_CP_Overflow"] : 0) + Environment.NewLine;
            //sass += "No Obligatoris -> Dia Opcional CP Forsant : " + (assignats.ContainsKey("NOOB_Opcional_CP_Marge_Overflow") ? assignats["NOOB_Opcional_CP_Marge_Overflow"] : 0) + Environment.NewLine;
            sass += "No Obligatoris -> Dia Opcional Zona : " + (assignats.ContainsKey("NOOB_Opcional_Zona") ? assignats["NOOB_Opcional_Zona"] : 0) + Environment.NewLine;
            sass += "No Obligatoris -> Dia Opcional Zona Forsant : " + (assignats.ContainsKey("NOOB_Opcional_Zona_Forsant") ? assignats["NOOB_Opcional_Zona_Forsant"] : 0) + Environment.NewLine;
            sass += "No Obligatoris -> Dia Opcional Fora Zona : " + (assignats.ContainsKey("NOOB_Opcional_NOCP_Nozona") ? assignats["NOOB_Opcional_NOCP_Nozona"] : 0) + Environment.NewLine;*/
            sass += "No Obligatoris -> KO : " + (assignats.ContainsKey("NOOB_KO") ? assignats["NOOB_KO"] : 0) + Environment.NewLine;
            return sass;
        }

        private void FerLog(StreamWriter f)
        {
            List<Tuple<Bucket, DateTime>> l = new List<Tuple<Bucket, DateTime>>();
            foreach (var b in buckets) l.Add(new Tuple<Bucket, DateTime>(b, b.Dia.Value));
            l.Sort(new Comparison<Tuple<Bucket, DateTime>>((a1, b1) => Math.Sign((a1.Item2 - b1.Item2).TotalDays)));

            foreach (var b in l)
            {
                f.Write("B\t" + b.Item1.zona + "\t" + b.Item1.Dia.Value.ToShortDateString());
                foreach (var cp in b.Item1.cpAssignats) f.Write(" " + cp.Id);
                f.WriteLine();
            }
            f.WriteLine();
        }

        #region V2
        public void Planificar_v2(bool tenir_en_compte_data_ultima_visita)
        {
            StreamWriter f = new StreamWriter(nom_fitxer_log, false); f.Close();

            // Crear llista de serveis a planificar (sense POU)
            CrearLlistesServeis();

            List<Bucket> buckets_trams = new List<Bucket>();
            var trams = TramsPlanificacio();
            var ultimes_visites_original = new Dictionary<string, DateTime>();
            foreach (var s in Context.Projecte.Serveis.Values) ultimes_visites_original.Add(s.Identificador, s.UltimaVisita);
            foreach (var tram in trams)
            {
                FerBuckets(tram.Item1, tram.Item2, tram.Item3);

                buckets_trams.AddRange(buckets);

                foreach (var b in buckets)
                    foreach (var s in b.serveis)
                        if (b.Dia.Value > Context.Projecte.Serveis[s.Identificador].UltimaVisita) Context.Projecte.Serveis[s.Identificador].UltimaVisita = b.Dia.Value;
            }

            buckets = buckets_trams;
            foreach (var s in Context.Projecte.Serveis.Values)
                s.UltimaVisita = ultimes_visites_original[s.Identificador];

            f = new StreamWriter(nom_fitxer_log, true);
            Dictionary<string, List<string>> lbs = new Dictionary<string, List<string>>();
            foreach (var b in buckets)
                foreach (var s in b.serveis)
                {
                    if (!lbs.ContainsKey(b.Dia.Value.ToShortDateString())) lbs.Add(b.Dia.Value.ToShortDateString(), new List<string>());
                    lbs[b.Dia.Value.ToShortDateString()].Add(s.Identificador);
                }
            foreach (var kv in lbs)
            {
                f.Write("SB\t" + kv.Key + "\t");
                for (int i = 0; i < kv.Value.Count; i++)
                {
                    if (i > 0) f.Write(";");
                    f.Write(kv.Value[i]);
                }
                f.WriteLine();
            }
            f.WriteLine();
            f.Close();

            MinimitzarFestius();
            if (tenir_en_compte_data_ultima_visita) ServeisEndarrerits(true);

            // Crear Planificacions
            PlanificacionsPOU();

            Planificacions();
        }

        void FerBuckets(DateTime inici, DateTime final, HashSet<string> vehicles)
        {
            Random r = new Random(0);
            var serveis_zones = new Dictionary<string, List<Servei>>();
            foreach (var s in llista_serveis)
            {
                var zona = Context.AssignarZona(s);
                if (!serveis_zones.ContainsKey(zona)) serveis_zones.Add(zona, new List<Servei>());
                serveis_zones[zona].Add(s);
            }
            var vehicles_zones = new Dictionary<string, List<Vehicle>>();
            foreach (var idv in vehicles)
            {
                var v = Context.Projecte.Vehicles[idv];
                var zona = Context.AssignarZona(v);
                if (!vehicles_zones.ContainsKey(zona)) vehicles_zones.Add(zona, new List<Vehicle>());
                vehicles_zones[zona].Add(v);
            }

            foreach (var zona in serveis_zones)
            {
                AGV a = new AGV(zona.Key, zona.Value, inici, final, vehicles_zones[zona.Key], r);
                a.Run();
            }
        }
        #endregion
    }

    class AGV
    {
        string Zona;
        List<Servei> Serveis;
        DateTime Inici;
        DateTime Final;
        List<Vehicle> Vehicles;
        Random R;
        List<SolV> Poblacio;
        Dictionary<string, GrupV> Grups;
        int Elitisme = 1;

        const int MidaPoblacio = 1000;

        public AGV(string zona, List<Servei> serveis, DateTime inici, DateTime final, List<Vehicle> vehicles, Random r)
        {
            Zona = zona;
            Serveis = serveis;
            Inici = inici;
            Final = final;
            Vehicles = vehicles;
            R = r;

            Pregrups();
            //Pregrups1();
        }

        void Pregrups()
        {
            Grups = new Dictionary<string, GrupV>();
            foreach (var s in Serveis)
            {
                var tipus = s.CodiPostal + ";" + s.Frequencia.TotalDays + ";" + s.Preferencies;
                if (!Grups.ContainsKey(tipus))
                {
                    var g = new GrupV();
                    g.CP = s.CodiPostal;
                    g.Frequencia = s.Frequencia;
                    g.Preferencies = s.Preferencies;
                    Grups.Add(tipus, g);
                }
                Grups[tipus].Serveis.Add(s);
            }
        }

        void Pregrups1()
        {
            Grups = new Dictionary<string, GrupV>();
            foreach (var s in Serveis)
            {
                var g = new GrupV();
                Grups.Add(s.Identificador, g);
                Grups[s.Identificador].Serveis.Add(s);
            }
        }

        public void Run()
        {
            StreamWriter f = new StreamWriter("logagv.txt", false); f.Close();
            PoblacioInicial(MidaPoblacio);
            for (int ig = 0; ig < 1000; ig++)
            {
                Creuament();
                Mutacio(0.01);
                Ordenar();
                Mort();

                f = new StreamWriter("logagv.txt", true); f.WriteLine(Poblacio[0].Text()); f.Close();
            }
        }

        void PoblacioInicial(int n)
        {
            Poblacio = new List<SolV>();
            for (int i = 0; i < n; i++)
            {
                var solV = new SolV(R);
                solV.Aleatoria(Grups.Values.ToList(), Inici, Final);
                solV.CalcularFitness();
                Poblacio.Add(solV);
            }
        }

        void Creuament()
        {
            int npares = Poblacio.Count;
            for (int i = 0; i < npares; i++)
            {
                int j = R.Next(Poblacio.Count);
                Creuar(Poblacio[i], Poblacio[j]);
            }
        }

        void Creuar(SolV a, SolV b)
        {
            var fill = new SolV(R);
            int idx = R.Next(a.Gens.Count);
            for (int i = 0; i < idx; i++)
            {
                fill.Gens.Add(a.Gens[i].Copia());
            }
            for (int i = idx; i < b.Gens.Count; i++)
            {
                fill.Gens.Add(b.Gens[i].Copia());
            }
            fill.CalcularFitness();
            Poblacio.Add(fill);
        }

        void Mutacio(double probabilitat)
        {
            foreach (var p in Poblacio)
            {
                double d = R.NextDouble();
                if (d < probabilitat)
                {
                    Mutar(p);
                }
            }
        }

        void Mutar(SolV s)
        {
            int idx = R.Next(s.Gens.Count);
            var gen = s.Gens[idx];
            int ndies = R.Next(10);
            var nou_dia = gen.Grup.DecidirDiaAleatori(gen.Dies[0].AddDays(-ndies), gen.Dies[0].AddDays(ndies), R);
            if (nou_dia != null)
            {
                int difdies = (int)(nou_dia.Value - gen.Dies[0]).TotalDays;
                for (int i = 0; i < gen.Dies.Count; i++)
                {
                    gen.Dies[i] = gen.Dies[i].AddDays(difdies);
                    if (gen.Dies[i] < Inici || gen.Dies[i] > Final)
                    {
                        gen.Dies.RemoveAt(i);
                        i--;
                    }
                }
                s.CalcularFitness();
            }
        }

        void Ordenar()
        {
            var ordre = new List<Tuple<SolV, double>>();
            foreach (var p in Poblacio)
            {
                double fitness = p.Fitness;
                ordre.Add(Tuple.Create(p, fitness));
            }
            ordre.Sort(new Comparison<Tuple<SolV, double>>((a1, b1) => Math.Sign(a1.Item2 - b1.Item2)));
            Poblacio.Clear();
            foreach (var o in ordre) Poblacio.Add(o.Item1);
        }

        void Mort()
        {
            while (Poblacio.Count > MidaPoblacio)
            {
                int idx = (int)(Elitisme + Math.Pow(R.Next(Poblacio.Count), 2));
                if (idx > Poblacio.Count - 1) idx = Poblacio.Count - 1;
                Poblacio.RemoveAt(idx);
            }
        }
    }

    class SolV
    {
        public List<GenV> Gens;
        Random R;
        double Saturacio = 25;
        public double Fitness = 0;

        public SolV(Random r)
        {
            R = r;
            Gens = new List<GenV>();
        }

        public void Aleatoria(List<GrupV> grups, DateTime inici, DateTime final)
        {
            foreach (var g in grups)
            {
                var dia = g.DecidirDiaAleatori(inici, final, R);
                var gen = new GenV(g);
                gen.Planificar(dia.Value, final);
                Gens.Add(gen);
            }
        }

        Dictionary<DateTime, List<GrupV>> Calendari()
        {
            var calendari = new Dictionary<DateTime, List<GrupV>>();
            foreach (var gen in Gens)
            {
                foreach (var dia in gen.Dies)
                {
                    if (!calendari.ContainsKey(dia)) calendari.Add(dia, new List<GrupV>());
                    calendari[dia].Add(gen.Grup);
                }
            }
            return calendari;
        }

        public void CalcularFitness()
        {
            double f = 0;
            var calendari = Calendari();
            int n = 0;
            foreach (var dia in calendari)
            {
                int ns = 0;
                foreach (var g in dia.Value)
                {
                    ns += g.Serveis.Count;
                }
                f += Math.Abs(Saturacio - ns);
                f += DistanciaGrups(dia.Value);
                n++;
            }
            Fitness = f / n;
        }

        public double DistanciaGrups(List<GrupV> grups)
        {
            double xmin = -1, xmax = -1, ymin = -1, ymax = -1;
            bool primer = true;
            foreach (var g in grups)
                foreach (var s in g.Serveis)
                {
                    if (primer || s.Adreça.X < xmin) xmin = s.Adreça.X;
                    if (primer || s.Adreça.Y < ymin) ymin = s.Adreça.Y;
                    if (primer || s.Adreça.X > xmax) xmax = s.Adreça.X;
                    if (primer || s.Adreça.Y > ymax) ymax = s.Adreça.Y;
                    if (primer) primer = false;
                }
            double val = Context.DistanciaKm(ymin, xmin, ymax, xmax);
            return val;
        }

        public override string ToString()
        {
            return Math.Round(Fitness, 3).ToString();
        }

        public string Text()
        {
            string s = Math.Round(Fitness, 3).ToString();
            var c = Calendari();
            var dies = c.Keys.ToList();
            dies.Sort();
            foreach (var d in dies)
            {
                int ns = 0;
                foreach (var g in c[d])
                {
                    ns += g.Serveis.Count;
                }
                s += " " + d.Day + "(" + ns + "-" + Math.Round(DistanciaGrups(c[d])) + ")";
            }
            return s;
        }
    }

    class GenV
    {
        public List<DateTime> Dies;
        public GrupV Grup;

        public GenV(GrupV grup)
        {
            Grup = grup;
            Dies = new List<DateTime>();
        }

        public GenV Copia()
        {
            var g = new GenV(Grup);
            g.Dies = new List<DateTime>();
            g.Dies.AddRange(Dies);
            return g;
        }

        public void Planificar(DateTime dia_inicial, DateTime dia_final)
        {
            DateTime d = dia_inicial;
            bool final = false;
            while (!final)
            {
                Dies.Add(d);
                d = d.AddDays(Grup.Frequencia.TotalDays * 7 / 5);
                if (d > dia_final) break;
            }
        }

        public override string ToString()
        {
            string s = Grup.CP;
            for (int i = 0; i < Dies.Count; i++)
            {
                s += " " + Dies[i].ToShortDateString();
            }
            return s;
        }
    }

    class GrupV
    {
        public List<Servei> Serveis;
        public string Preferencies;
        public string CP;
        public TimeSpan Frequencia;

        public GrupV()
        {
            Serveis = new List<Servei>();
        }

        public DateTime? DecidirDiaAleatori(DateTime inici, DateTime final, Random r)
        {
            DateTime dia = inici;
            dia = dia.AddDays(r.Next((int)(Frequencia.TotalDays * 7 / 5)));

            bool correcte = true;
            do
            {
                correcte = true;
                int indexdia = -1;
                switch (dia.DayOfWeek)
                {
                    case DayOfWeek.Monday: indexdia = 0; break;
                    case DayOfWeek.Tuesday: indexdia = 1; break;
                    case DayOfWeek.Wednesday: indexdia = 2; break;
                    case DayOfWeek.Thursday: indexdia = 3; break;
                    case DayOfWeek.Friday: indexdia = 4; break;
                    case DayOfWeek.Saturday: indexdia = 5; break;
                    case DayOfWeek.Sunday: indexdia = 6; break;
                }
                if (indexdia > 4) correcte = false;
                else if (indexdia < Preferencies.Length && (Preferencies[indexdia] == 'T')) correcte = false;
                else if (indexdia < Preferencies.Length && (Preferencies[indexdia] == 'X')) correcte = false;
                else if (Context.FestiuBP(dia)) correcte = false;
                if (!correcte) dia = dia.AddDays(1);
                if (dia > final) return null;
            } while (!correcte);
            return dia;
        }
    }
}
