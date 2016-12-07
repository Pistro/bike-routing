# -*- coding: utf-8 -*-
from __future__ import unicode_literals

from django.db import models, migrations


class Migration(migrations.Migration):

    dependencies = [
    ]

    operations = [
        migrations.CreateModel(
            name='Membership',
            fields=[
                ('id', models.AutoField(serialize=False, verbose_name='ID', primary_key=True, auto_created=True)),
                ('pos', models.IntegerField(verbose_name='pos')),
            ],
        ),
        migrations.CreateModel(
            name='Node',
            fields=[
                ('id', models.IntegerField(serialize=False, verbose_name='id', primary_key=True)),
                ('lat', models.FloatField(verbose_name='latitude')),
                ('lon', models.FloatField(verbose_name='longitude')),
                ('height', models.DecimalField(default=0, decimal_places=2, verbose_name='height', max_digits=7)),
            ],
        ),
        migrations.CreateModel(
            name='Tag',
            fields=[
                ('id', models.IntegerField(serialize=False, verbose_name='id', primary_key=True)),
                ('key', models.TextField(verbose_name='key')),
                ('value', models.TextField(verbose_name='value')),
            ],
        ),
        migrations.CreateModel(
            name='Way',
            fields=[
                ('id', models.IntegerField(serialize=False, verbose_name='id', primary_key=True)),
                ('orgWayId', models.IntegerField(verbose_name='orgWayId')),
                ('orgWaySubId', models.IntegerField(verbose_name='orgWaySubId')),
                ('nodes', models.ManyToManyField(db_constraint='nodes', through='showRoutes.Membership', to='showRoutes.Node')),
                ('tags', models.ManyToManyField(db_constraint='tags', to='showRoutes.Tag', blank=True)),
            ],
        ),
        migrations.AddField(
            model_name='membership',
            name='node',
            field=models.ForeignKey(to='showRoutes.Node'),
        ),
        migrations.AddField(
            model_name='membership',
            name='way',
            field=models.ForeignKey(to='showRoutes.Way'),
        ),
    ]
